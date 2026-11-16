package com.liquiddebloat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.liquiddebloat.data.DebloatRepository
import com.liquiddebloat.data.RootShell
import com.liquiddebloat.data.SettingsStore
import com.liquiddebloat.model.AppEntry
import com.liquiddebloat.ui.AppList
import com.liquiddebloat.ui.EmptyState
import com.liquiddebloat.ui.GlassIconButton
import com.liquiddebloat.ui.GlassPalette
import com.liquiddebloat.ui.GlassSurface
import com.liquiddebloat.ui.GlassTag
import com.liquiddebloat.ui.LiquidBackground
import com.liquiddebloat.ui.ScopeTabs
import com.liquiddebloat.ui.SearchField
import com.liquiddebloat.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { AppRoot() }
    }
}

private enum class Screen { APPS, SETTINGS }

private const val TAB_ALL = 0
private const val TAB_USER = 1
private const val TAB_SYSTEM = 2
private const val TAB_REMOVED = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val repository = remember { DebloatRepository(context) }
    val settings = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()

    val versionLabel = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    var screen by remember { mutableStateOf(Screen.APPS) }
    var tab by remember { mutableStateOf(TAB_ALL) }
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var rootAvailable by remember { mutableStateOf<Boolean?>(null) }
    var confirmTarget by remember { mutableStateOf<AppEntry?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var busyPackage by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var menuEntry by remember { mutableStateOf<AppEntry?>(null) }

    suspend fun reload() {
        loading = true
        rootAvailable = withContext(Dispatchers.IO) { RootShell.isRootAvailable() }
        allApps = repository.loadApps(DebloatRepository.Scope.ALL)
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2400)
            toast = null
        }
    }

    val userCount = allApps.count { !it.isSystem && !it.isUninstalled }
    val systemCount = allApps.count { it.isSystem && !it.isUninstalled }
    val removedCount = allApps.count { it.isUninstalled }

    val scoped = remember(allApps, tab) {
        when (tab) {
            TAB_USER -> allApps.filter { !it.isSystem && !it.isUninstalled }
            TAB_SYSTEM -> allApps.filter { it.isSystem && !it.isUninstalled }
            TAB_REMOVED -> allApps.filter { it.isUninstalled }
            else -> allApps.filter { !it.isUninstalled } + allApps.filter { it.isUninstalled }
        }
    }

    val filtered = remember(scoped, query) {
        if (query.isBlank()) scoped else scoped.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    /** Runs one uninstall (or restore) and reflects the result immediately. */
    fun runAction(entry: AppEntry) {
        busyPackage = entry.packageName
        scope.launch {
            val message = if (entry.isUninstalled) {
                repository.restore(entry)
            } else {
                repository.uninstall(entry, settings.strongMode, settings.backupEnabled)
            }
            busyPackage = null
            toast = "${entry.label}：${message.lineSequence().first()}"
            allApps = allApps.map {
                if (it.packageName == entry.packageName) it.copy(isUninstalled = !entry.isUninstalled) else it
            }
            // Re-read the package table so paths / labels stay correct.
            allApps = withContext(Dispatchers.IO) { repository.loadApps(DebloatRepository.Scope.ALL) }
        }
    }

    fun onAction(entry: AppEntry) {
        when {
            entry.isUninstalled -> runAction(entry)
            entry.packageName in repository.protectedPackages ->
                toast = "${entry.label} 在受保护名单里，已拦截"
            settings.strongMode -> confirmTarget = entry
            else -> runAction(entry)
        }
    }

    fun copyToClipboard(text: String, what: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        runCatching { cm.setPrimaryClip(ClipData.newPlainText(what, text)) }
        menuEntry = null
        toast = "已复制$what"
    }

    fun openBackupFolder() {
        scope.launch {
            withContext(Dispatchers.IO) { RootShell.run("mkdir -p /sdcard/LiquidDebloat/backup") }
            val docUri = Uri.parse(
                "content://com.android.externalstorage.documents/document/primary%3ALiquidDebloat%2Fbackup"
            )
            var opened = false
            for (mime in listOf("vnd.android.document/directory", "resource/folder")) {
                val view = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(docUri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (view.resolveActivity(context.packageManager) == null) continue
                if (runCatching { context.startActivity(view) }.isSuccess) {
                    opened = true
                    break
                }
            }
            if (!opened) {
                val tree = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra("android.provider.extra.INITIAL_URI", docUri)
                }
                if (runCatching { context.startActivity(tree) }.isFailure) {
                    toast = "没有找到能打开目录的文件管理器"
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LiquidBackground()

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Header(
                screen = screen,
                rootAvailable = rootAvailable,
                total = allApps.size,
                onBack = { screen = Screen.APPS },
                onRefresh = { scope.launch { reload() } },
                onSettings = { screen = Screen.SETTINGS },
            )
            Spacer(Modifier.height(14.dp))

            if (screen == Screen.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    protectedCount = repository.protectedPackages.size,
                    backupRoot = repository.backupRoot(),
                    versionLabel = versionLabel,
                    onOpenBackupFolder = { openBackupFolder() },
                )
            } else {
                SearchField(query = query, onQueryChange = { query = it })
                Spacer(Modifier.height(9.dp))
                ScopeTabs(
                    tabs = listOf(
                        "全部" to allApps.size,
                        "用户" to userCount,
                        "系统" to systemCount,
                        "已卸载" to removedCount,
                    ),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                )
                Spacer(Modifier.height(12.dp))

                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            rootAvailable = withContext(Dispatchers.IO) { RootShell.isRootAvailable() }
                            allApps = withContext(Dispatchers.IO) {
                                repository.loadApps(DebloatRepository.Scope.ALL)
                            }
                            refreshing = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GlassPalette.accent, strokeWidth = 2.dp)
                        }

                        filtered.isEmpty() -> EmptyState(
                            text = when {
                                query.isNotBlank() -> "没有匹配「$query」的应用"
                                rootAvailable == false && allApps.isEmpty() ->
                                    "没读到应用列表，请确认已授予 root 或无限制读取应用列表的权限"

                                tab == TAB_REMOVED -> "还没有卸载过应用"
                                else -> "这里是空的"
                            },
                        )

                        else -> AppList(
                            apps = filtered,
                            protectedPackages = repository.protectedPackages,
                            busyPackage = busyPackage,
                            onAction = { onAction(it) },
                            onLongPress = { menuEntry = it },
                        )
                    }
                }
            }
        }

        // Toast
        val currentToast = toast
        if (currentToast != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
            ) {
                GlassSurface(corner = 100.dp) {
                    Text(
                        currentToast,
                        color = GlassPalette.textPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                    )
                }
            }
        }

        if (busyPackage != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                GlassSurface(corner = 100.dp) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = GlassPalette.accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(14.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("正在处理…", color = GlassPalette.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    val menu = menuEntry
    if (menu != null) {
        AlertDialog(
            onDismissRequest = { menuEntry = null },
            containerColor = Color(0xFFF9FBFE),
            titleContentColor = GlassPalette.textPrimary,
            textContentColor = GlassPalette.textSecondary,
            title = { Text(menu.label, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(menu.packageName, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(14.dp))
                    CopyRow("复制应用名称", menu.label) { copyToClipboard(menu.label, "应用名称") }
                    Spacer(Modifier.height(8.dp))
                    CopyRow("复制包名", menu.packageName) { copyToClipboard(menu.packageName, "包名") }
                }
            },
            confirmButton = {
                TextButton(onClick = { menuEntry = null }) {
                    Text("取消", color = GlassPalette.textSecondary)
                }
            },
        )
    }

    // Only destructive (strong) mode asks for confirmation; safe mode is reversible.
    val target = confirmTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { confirmTarget = null },
            containerColor = Color(0xFFF9FBFE),
            titleContentColor = GlassPalette.textPrimary,
            textContentColor = GlassPalette.textSecondary,
            title = { Text("彻底删除 ${target.label}？", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "会先卸载当前用户，再尝试删除分区内的 APK 目录（${target.apkPath ?: "路径未知"}）。" +
                        "这一步不可逆，只能靠备份重装或线刷恢复。",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmTarget = null
                    runAction(target)
                }) { Text("仍然删除", color = GlassPalette.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmTarget = null }) {
                    Text("取消", color = GlassPalette.textSecondary)
                }
            },
        )
    }

    val result = resultText
    if (result != null) {
        AlertDialog(
            onDismissRequest = { resultText = null },
            containerColor = Color(0xFFF9FBFE),
            titleContentColor = GlassPalette.textPrimary,
            textContentColor = GlassPalette.textSecondary,
            title = { Text("操作结果", fontWeight = FontWeight.SemiBold) },
            text = { Text(result, fontSize = 11.sp, lineHeight = 17.sp) },
            confirmButton = {
                TextButton(onClick = { resultText = null }) {
                    Text("好", color = GlassPalette.accent)
                }
            },
        )
    }
}

@Composable
private fun Header(
    screen: Screen,
    rootAvailable: Boolean?,
    total: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (screen == Screen.SETTINGS) {
            GlassIconButton(Icons.Rounded.ArrowBack, "返回", onClick = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = if (screen == Screen.SETTINGS) "设置" else "小卸载",
                color = GlassPalette.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (screen == Screen.SETTINGS) {
                    "备份、模式与安全策略"
                } else if (total > 0) {
                    "$total 个应用 · 一键卸载预装软件"
                } else {
                    "root 权限下一键清理预装应用"
                },
                color = GlassPalette.textSecondary,
                fontSize = 11.sp,
            )
        }
        when (rootAvailable) {
            true -> GlassTag("Root", GlassPalette.success)
            false -> GlassTag("无 Root", GlassPalette.danger)
            null -> GlassTag("检测中", GlassPalette.textTertiary)
        }
        Spacer(Modifier.width(8.dp))
        if (screen == Screen.APPS) {
            GlassIconButton(Icons.Rounded.Refresh, "刷新列表", onClick = onRefresh)
            Spacer(Modifier.width(8.dp))
            GlassIconButton(Icons.Rounded.Settings, "设置", onClick = onSettings)
        }
    }
}

@Composable
private fun CopyRow(text: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = GlassPalette.accent,
            modifier = Modifier.width(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = GlassPalette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, color = GlassPalette.textTertiary, fontSize = 10.sp, maxLines = 1)
        }
    }
}
