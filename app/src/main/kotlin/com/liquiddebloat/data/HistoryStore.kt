package com.liquiddebloat.data

import android.content.Context
import com.liquiddebloat.model.AppEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local record of everything this app has uninstalled.
 *
 * `pm list packages -u` is the usual source for the 已卸载 tab, but it is not
 * reliable across ROMs: on some ColorOS builds the `-u` table is rebuilt and
 * older entries silently drop out. Keeping our own list means the user always
 * sees everything they removed, and we still know the label afterwards.
 */
class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("liquid_debloat_history", Context.MODE_PRIVATE)

    fun all(): List<Record> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Record>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val pkg = o.optString("pkg").ifEmpty { continue }
            out += Record(
                packageName = pkg,
                label = o.optString("label").ifEmpty { null },
                version = o.optString("version").ifEmpty { null },
                apkPath = o.optString("path").ifEmpty { null },
                system = o.optBoolean("system", false),
                removedAt = o.optLong("at", 0L),
            )
        }
        return out
    }

    fun record(entry: AppEntry) {
        val list = all().toMutableList()
        list.removeAll { it.packageName == entry.packageName }
        list.add(
            0,
            Record(
                packageName = entry.packageName,
                label = entry.label,
                version = entry.versionName,
                apkPath = entry.apkPath,
                system = entry.isSystem,
                removedAt = System.currentTimeMillis(),
            )
        )
        save(list)
    }

    fun remove(packageName: String) {
        val list = all().toMutableList()
        if (list.removeAll { it.packageName == packageName }) save(list)
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    private fun save(list: List<Record>) {
        val array = JSONArray()
        list.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("pkg", r.packageName)
                    put("label", r.label ?: "")
                    put("version", r.version ?: "")
                    put("path", r.apkPath ?: "")
                    put("system", r.system)
                    put("at", r.removedAt)
                }
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    data class Record(
        val packageName: String,
        val label: String?,
        val version: String?,
        val apkPath: String?,
        val system: Boolean,
        val removedAt: Long,
    )

    private companion object {
        const val KEY = "removed"
    }
}
