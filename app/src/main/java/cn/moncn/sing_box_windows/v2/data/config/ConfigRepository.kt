package cn.moncn.sing_box_windows.v2.data.config

import android.content.Context
import cn.moncn.sing_box_windows.vpn.VpnDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import cn.moncn.sing_box_windows.v2.domain.settings.AppSettings

object ConfigRepository {
    private const val FILE_NAME = "singbox.json"
    private val fileMutex = Mutex()

    suspend fun loadOrCreateConfig(context: Context): String = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            loadOrCreateConfigFrom(File(context.filesDir, FILE_NAME))
        }
    }

    /**
     * 读取（必要时创建 / 修复）配置文件。
     *
     * 抽成与 Android 无关的纯函数，便于单元测试覆盖「空文件 / 损坏文件自愈」。
     */
    internal fun loadOrCreateConfigFrom(file: File): String {
        if (!file.exists()) {
            val defaultJson = ConfigDefaults.defaultConfigJson()
            writeAtomically(file, defaultJson)
            return defaultJson
        }
        val raw = file.readText()
        // 空文件 / 损坏文件会导致内核报「unexpected end of JSON input」而永远连不上，
        // 这里直接重建兜底配置，避免用户陷入无法自愈的死局。
        if (raw.isBlank() || !isParsable(raw)) {
            val defaultJson = ConfigDefaults.defaultConfigJson()
            writeAtomically(file, defaultJson)
            return defaultJson
        }
        val migrated = ConfigMigrator.migrate(migrateTunAddress(raw))
        if (migrated != raw) {
            writeAtomically(file, migrated)
        }
        return migrated
    }

    private fun isParsable(json: String): Boolean {
        return runCatching { JSONObject(json) }.isSuccess
    }

    suspend fun saveConfig(context: Context, json: String) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val file = File(context.filesDir, FILE_NAME)
            val normalized = normalizeConfig(json)
            writeAtomically(file, normalized)
        }
    }

    suspend fun saveConfigWithSettings(context: Context, json: String, settings: AppSettings) =
        withContext(Dispatchers.IO) {
            fileMutex.withLock {
                val file = File(context.filesDir, FILE_NAME)
                val normalized = normalizeConfig(json)
                val applied = ConfigSettingsApplier.applySettings(normalized, settings)
                writeAtomically(file, applied)
            }
        }

    suspend fun applySettings(context: Context, settings: AppSettings) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val file = File(context.filesDir, FILE_NAME)
            val raw = if (file.exists()) file.readText() else ConfigDefaults.defaultConfigJson()
            val normalized = normalizeConfig(raw)
            val applied = ConfigSettingsApplier.applySettings(normalized, settings)
            writeAtomically(file, applied)
        }
    }

    suspend fun convertClashAndSave(context: Context, clashYaml: String): ConversionResult {
        val result = ClashConverter.convert(clashYaml)
        saveConfig(context, result.json)
        return result
    }

    private fun writeAtomically(file: File, content: String) {
        val parent = file.parentFile ?: error("invalid file path: ${file.absolutePath}")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, "${file.name}.tmp")
        tempFile.writeText(content)

        // 同目录重命名，尽量保证配置写入的完整性
        if (!tempFile.renameTo(file)) {
            file.writeText(content)
            tempFile.delete()
        }
    }

    private fun migrateTunAddress(json: String): String {
        return runCatching {
            val root = JSONObject(json)
            var changed = false
            // 内核格式迁移（含 TUN 地址归一化：sing-box 1.11 → 1.14）
            changed = changed or ConfigMigrator.migrate(root)
            changed = changed or ensureOutboundGroup(root)
            if (changed) root.toString(2) else json
        }.getOrElse { json }
    }

    private fun normalizeConfig(json: String): String {
        return runCatching {
            val root = JSONObject(json)
            var changed = false
            changed = changed or ensureOutboundGroup(root)
            // 内核格式迁移（sing-box 1.11 → 1.14）：移除已废弃的字段
            changed = changed or ConfigMigrator.migrate(root)
            if (changed) root.toString(2) else json
        }.getOrElse { json }
    }

    private fun ensureOutboundGroup(root: JSONObject): Boolean {
        val outbounds = root.optJSONArray("outbounds") ?: return false
        var changed = false
        val existingTags = mutableSetOf<String>()
        val groupTypes = setOf("selector", "urltest", "fallback")
        var firstGroupTag: String? = null
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val tag = outbound.optString("tag")
            if (tag.isNotBlank()) {
                existingTags.add(tag)
            }
            val type = outbound.optString("type")
            if (type in groupTypes && firstGroupTag == null) {
                firstGroupTag = tag
            }
        }

        if (firstGroupTag == null) {
            val proxyTags = mutableListOf<String>()
            val excludedTypes = setOf("direct", "block", "dns")
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                val tag = outbound.optString("tag")
                val type = outbound.optString("type")
                if (tag.isNotBlank() && type !in excludedTypes) {
                    proxyTags.add(tag)
                }
            }
            if (proxyTags.isEmpty()) {
                return changed
            }
            val groupTag = uniqueTag("Proxy", existingTags)
            val groupOutbound = JSONObject()
                .put("type", "selector")
                .put("tag", groupTag)
                .put("outbounds", JSONArray().apply { proxyTags.forEach { put(it) } })
                .put("default", proxyTags.first())
            outbounds.put(groupOutbound)
            existingTags.add(groupTag)
            firstGroupTag = groupTag
            changed = true
        }

        val route = root.optJSONObject("route") ?: JSONObject().also {
            root.put("route", it)
            changed = true
        }
        val resolvedGroupTag = firstGroupTag ?: return changed
        val finalTag = route.optString("final")
        if (finalTag.isBlank() || finalTag == "DIRECT") {
            route.put("final", resolvedGroupTag)
            changed = true
        }

        return changed
    }

    private fun uniqueTag(base: String, existing: Set<String>): String {
        if (!existing.contains(base)) {
            return base
        }
        var index = 2
        while (true) {
            val candidate = "$base-$index"
            if (!existing.contains(candidate)) {
                return candidate
            }
            index++
        }
    }
}
