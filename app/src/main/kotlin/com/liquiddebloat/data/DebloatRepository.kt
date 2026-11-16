package com.liquiddebloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.liquiddebloat.model.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DebloatRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val history = HistoryStore(context)

    /** Package names that must never be touched: breaking them soft-bricks the device. */
    val protectedPackages: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.android.providers.downloads",
        "com.android.permissioncontroller",
        "com.android.shell",
        "com.android.packageinstaller",
        "com.android.keychain",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.qualcomm.qti.telephonyservice",
        "com.oplus.wallpapers",
    )

    // ---------------------------------------------------------------- listing

    suspend fun loadApps(scope: Scope): List<AppEntry> = withContext(Dispatchers.IO) {
        // With root we read the full package table straight from pm (this is also
        // the only way to see packages uninstalled for the current user).
        // Without root we still want a usable, read-only list, so fall back to
        // PackageManager (needs QUERY_ALL_PACKAGES on Android 11+).
        val base = if (RootShell.isRootAvailable()) loadFromShell(scope) else loadFromPackageManager(scope)
        mergeHistory(scope, base)
    }

    /** Adds entries we removed earlier that pm no longer reports. */
    private fun mergeHistory(scope: Scope, base: List<AppEntry>): List<AppEntry> {
        if (scope == Scope.USER || scope == Scope.SYSTEM) return base
        val known = base.mapTo(HashSet()) { it.packageName }
        val extras = history.all()
            .filter { it.packageName !in known }
            .map { rec ->
                AppEntry(
                    packageName = rec.packageName,
                    label = rec.label ?: backupLabel(rec.packageName) ?: rec.packageName,
                    apkPath = rec.apkPath,
                    versionName = rec.version,
                    isSystem = rec.system,
                    isUninstalled = true,
                    icon = rec.apkPath?.let { archiveIcon(it) },
                )
            }
        if (extras.isEmpty()) return base
        return (base + extras).sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
    }

    private fun archiveIcon(apkPath: String): ImageBitmap? = runCatching {
        val ai = pm.getPackageArchiveInfo(apkPath, 0)?.applicationInfo ?: return null
        ai.sourceDir = apkPath
        ai.publicSourceDir = apkPath
        pm.getApplicationIcon(ai).toImageBitmap(144)
    }.getOrNull()

    private fun loadFromShell(scope: Scope): List<AppEntry> {
        // "pm list packages -f -u" is supposed to return paths for removed apps
        // too, but several vendor ROMs (ColorOS included) rebuild that table and
        // drop older entries. Query the sets separately and take the union.
        val withPaths = RootShell.run("pm list packages -f")?.output.orEmpty()
        val allNames = RootShell.run("pm list packages -u")?.output.orEmpty()
        val installedRaw = RootShell.run("pm list packages")?.output.orEmpty()
        val systemRaw = RootShell.run("pm list packages -s")?.output.orEmpty()

        if (withPaths.isBlank() && allNames.isBlank()) return emptyList()

        val pathByPackage = LinkedHashMap<String, String>()
        for (line in withPaths.lineSequence()) {
            val trimmed = line.trim().removePrefix("package:")
            val eq = trimmed.lastIndexOf('=')
            if (eq <= 0) continue
            val path = trimmed.substring(0, eq)
            val pkg = trimmed.substring(eq + 1)
            if (pkg.isNotEmpty()) pathByPackage[pkg] = path
        }

        val installed = installedRaw.names()
        val system = systemRaw.names()
        val everything = LinkedHashSet<String>()
        everything += pathByPackage.keys
        everything += allNames.names()
        everything += installed

        if (everything.isEmpty()) return emptyList()

        return buildEntries(
            scope = scope,
            packages = everything.map { pkg ->
                PackageRef(
                    packageName = pkg,
                    apkPath = pathByPackage[pkg],
                    installed = pkg in installed,
                    system = pkg in system,
                )
            },
        )
    }

    private fun String.names(): HashSet<String> = lineSequence()
        .mapNotNull { it.removePrefix("package:").trim().ifEmpty { null } }
        .toHashSet()

    private fun loadFromPackageManager(scope: Scope): List<AppEntry> {
        if (scope == Scope.REMOVED) return emptyList()
        val info = runCatching { pm.getInstalledPackages(PackageManager.GET_META_DATA) }
            .getOrDefault(emptyList())
        return buildEntries(
            scope = scope,
            packages = info.mapNotNull { pi ->
                val ai = pi.applicationInfo ?: return@mapNotNull null
                val system = (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                PackageRef(
                    packageName = pi.packageName,
                    apkPath = ai.publicSourceDir,
                    installed = true,
                    system = system,
                )
            },
        )
    }

    private fun buildEntries(scope: Scope, packages: List<PackageRef>): List<AppEntry> {
        val ownPackages = setOf(context.packageName, "com.liquiddebloat", "com.liquiddebloat.debug")
        return packages.mapNotNull { ref ->
            if (ref.packageName in ownPackages) return@mapNotNull null
            val matchesScope = when (scope) {
                Scope.ALL -> true
                Scope.USER -> ref.installed && !ref.system
                Scope.SYSTEM -> ref.installed && ref.system
                Scope.REMOVED -> !ref.installed
            }
            if (!matchesScope) return@mapNotNull null

            // Uninstalled-for-user packages are invisible to getPackageInfo, so
            // read their label/icon straight out of the APK archive instead; if
            // even the APK is gone, fall back to the backup meta.txt we wrote.
            val meta = resolveMeta(ref.packageName, ref.apkPath)
            val label = meta.label
                ?: (if (!ref.installed) backupLabel(ref.packageName) else null)
                ?: ref.packageName

            AppEntry(
                packageName = ref.packageName,
                label = label,
                apkPath = ref.apkPath,
                versionName = meta.version,
                isSystem = ref.system,
                isUninstalled = !ref.installed,
                icon = meta.icon,
            )
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
    }

    private data class Meta(val label: String?, val version: String?, val icon: ImageBitmap?)

    private fun resolveMeta(pkg: String, apkPath: String?): Meta {
        // Path 1: the package is installed for this user.
        runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()?.let { pi ->
            val ai = pi.applicationInfo
            val label = ai?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
            val icon = ai?.let { runCatching { pm.getApplicationIcon(it).toImageBitmap(144) }.getOrNull() }
            return Meta(label, pi.versionName, icon)
        }
        // Path 2: still on disk, just not installed for this user.
        val archive = apkPath?.let { runCatching { pm.getPackageArchiveInfo(it, 0) }.getOrNull() }
        val ai = archive?.applicationInfo
        if (archive != null && ai != null) {
            ai.sourceDir = apkPath
            ai.publicSourceDir = apkPath
            val label = runCatching { pm.getApplicationLabel(ai).toString() }.getOrNull()
            val icon = runCatching { pm.getApplicationIcon(ai).toImageBitmap(144) }.getOrNull()
            return Meta(label, archive.versionName, icon)
        }
        return Meta(null, null, null)
    }

    /** Reads the label we stored next to the backup APK, e.g. `label=微信`. */
    private fun backupLabel(pkg: String): String? {
        val file = "${backupDir(pkg)}/meta.txt"
        val result = RootShell.run("cat \"$file\" 2>/dev/null") ?: return null
        return result.output.lineSequence()
            .firstOrNull { it.startsWith("label=") }
            ?.removePrefix("label=")
            ?.trim()
            ?.ifEmpty { null }
    }

    private data class PackageRef(
        val packageName: String,
        val apkPath: String?,
        val installed: Boolean,
        val system: Boolean,
    )

    // -------------------------------------------------------------- operations

    suspend fun uninstall(entry: AppEntry, strong: Boolean, backup: Boolean): String =
        withContext(Dispatchers.IO) {
            if (entry.packageName in protectedPackages) {
                return@withContext "已拦截：${entry.label} 属于受保护应用，卸载会导致系统异常。"
            }
            val report = StringBuilder()

            if (backup) {
                report.appendLine(backupApk(entry))
            }

            val safe = RootShell.run("pm uninstall --user 0 ${entry.packageName}")
                ?: return@withContext "无法调用 su，请确认已授予 root 权限。"
            val safeOk = safe.output.contains("Success", ignoreCase = true) || safe.code == 0
            report.appendLine(if (safeOk) "已卸载（当前用户）" else "卸载失败：${safe.output.trim().take(120)}")
            if (safeOk) history.record(entry)

            if (strong && safeOk) {
                report.appendLine(deleteApkDirectory(entry))
            }
            report.toString().trim()
        }

    suspend fun restore(entry: AppEntry): String = withContext(Dispatchers.IO) {
        val first = RootShell.run("cmd package install-existing ${entry.packageName}")
            ?: return@withContext "无法调用 su，请确认已授予 root 权限。"
        if (first.output.contains("installed for user", ignoreCase = true) ||
            first.output.contains("Success", ignoreCase = true)
        ) {
            history.remove(entry.packageName)
            return@withContext "已恢复：${entry.label}"
        }
        val backupDir = backupDir(entry.packageName)
        val second = RootShell.run("pm install -r $backupDir/base.apk")
        if (second != null && second.output.contains("Success", ignoreCase = true)) {
            history.remove(entry.packageName)
            "已从备份安装：${entry.label}"
        } else {
            "恢复失败：${(second ?: first).output.trim().take(140)}"
        }
    }

    // ------------------------------------------------------------------ backup

    fun backupRoot(): String = "/sdcard/LiquidDebloat/backup"

    private fun backupDir(pkg: String): String = "${backupRoot()}/$pkg"

    private fun backupApk(entry: AppEntry): String {
        val apk = entry.apkPath
        if (apk.isNullOrBlank()) return "跳过备份：没有可读的 APK 路径"
        val dir = backupDir(entry.packageName)
        val commands = buildString {
            append("mkdir -p \"$dir\" && ")
            append("cp -f \"$apk\" \"$dir/base.apk\" && ")
            append("printf '%s\\n' \"pkg=${entry.packageName}\" \"label=${entry.label.replace('\'', ' ')}\" ")
            append("\"version=${entry.versionName ?: "-"}\" \"system=${entry.isSystem}\" > \"$dir/meta.txt\"")
        }
        val result = RootShell.run(commands) ?: return "备份失败：su 不可用"
        return if (result.ok) "已备份到 $dir" else "备份失败：${result.output.trim().take(100)}"
    }

    // ------------------------------------------------------------- strong mode

    private fun deleteApkDirectory(entry: AppEntry): String {
        val apk = entry.apkPath ?: return "彻底删除跳过：没有 APK 路径"
        if (!apk.startsWith("/system") && !apk.startsWith("/product") &&
            !apk.startsWith("/vendor") && !apk.startsWith("/odm") &&
            !apk.startsWith("/system_ext") && !apk.startsWith("/my_")
        ) {
            return "彻底删除跳过：$apk 不在只读分区"
        }
        val dir = apk.substringBeforeLast('/')
        val remount = listOf("/system", "/system_ext", "/product", "/vendor", "/odm", "/my_product")
            .joinToString(" ; ") { "mount -o rw,remount $it 2>/dev/null" }
        val command = "$remount ; rm -rf \"$dir\" ; ls \"$dir\" 2>&1 | head -1"
        val result = RootShell.run(command) ?: return "彻底删除失败：su 不可用"
        val stillThere = result.output.lineSequence().any { it.trim().isNotEmpty() && !it.contains("No such file") }
        return if (stillThere) "删除未生效：$dir 仍存在（分区可能只读）" else "已删除 APK 目录 $dir"
    }

    // ------------------------------------------------------------------- misc

    private fun Drawable.toImageBitmap(size: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bitmap.asImageBitmap()
    }

    enum class Scope { ALL, USER, SYSTEM, REMOVED }
}
