package cn.moncn.sing_box_windows.config

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class SubscriptionItem(
    val id: String,
    val name: String,
    val url: String,
    val lastUpdatedAt: Long? = null,
    val lastError: String? = null
)

data class SubscriptionState(
    val items: List<SubscriptionItem>,
    val selectedId: String?
) {
    fun selected(): SubscriptionItem? = items.firstOrNull { it.id == selectedId }

    companion object {
        fun empty(): SubscriptionState = SubscriptionState(emptyList(), null)
    }
}

data class SubscriptionUpdateResult(
    val ok: Boolean,
    val message: String,
    val warnings: List<String>,
    val state: SubscriptionState
)

object SubscriptionRepository {
    private const val FILE_NAME = "subscriptions.json"

    fun load(context: Context): SubscriptionState {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            return SubscriptionState.empty()
        }
        return runCatching {
            val root = JSONObject(file.readText())
            val selectedId = root.optString("selectedId").takeIf { it.isNotBlank() }
            val itemsJson = root.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<SubscriptionItem>()
            for (index in 0 until itemsJson.length()) {
                val obj = itemsJson.optJSONObject(index) ?: continue
                items.add(
                    SubscriptionItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        url = obj.optString("url"),
                        lastUpdatedAt = obj.optLong("lastUpdatedAt").takeIf { it > 0 },
                        lastError = obj.optString("lastError").takeIf { it.isNotBlank() }
                    )
                )
            }
            val validSelected = selectedId?.takeIf { id -> items.any { it.id == id } }
            val fallbackSelected = validSelected ?: items.firstOrNull()?.id
            SubscriptionState(items, fallbackSelected)
        }.getOrElse {
            SubscriptionState.empty()
        }
    }

    fun add(context: Context, name: String, url: String): SubscriptionState {
        val state = load(context)
        val cleanName = name.trim()
        val cleanUrl = url.trim()
        val displayName = cleanName.ifBlank { deriveName(cleanUrl, state.items.size + 1) }
        val newItem = SubscriptionItem(
            id = UUID.randomUUID().toString(),
            name = displayName,
            url = cleanUrl
        )
        val newState = SubscriptionState(state.items + newItem, newItem.id)
        save(context, newState)
        return newState
    }

    fun remove(context: Context, id: String): SubscriptionState {
        val state = load(context)
        val remaining = state.items.filterNot { it.id == id }
        val nextSelected = state.selectedId?.takeIf { it != id } ?: remaining.firstOrNull()?.id
        val newState = SubscriptionState(remaining, nextSelected)
        save(context, newState)
        return newState
    }

    fun select(context: Context, id: String): SubscriptionState {
        val state = load(context)
        val newState = SubscriptionState(state.items, id)
        save(context, newState)
        return newState
    }

    fun updateSelected(context: Context): SubscriptionUpdateResult {
        val state = load(context)
        val selected = state.selected()
            ?: return SubscriptionUpdateResult(
                ok = false,
                message = "请先选择订阅",
                warnings = emptyList(),
                state = state
            )
        return updateItem(context, selected)
    }

    private fun updateItem(context: Context, item: SubscriptionItem): SubscriptionUpdateResult {
        return try {
            val content = fetchSubscription(item.url)
            val result = TemplateConfigBuilder.buildFromSubscription(content)
            if (!result.ok || result.configJson == null) {
                throw IOException(result.error ?: "订阅解析失败")
            }
            ConfigRepository.saveConfig(context, result.configJson)
            val message = if (result.warnings.isNotEmpty()) {
                "订阅更新完成，但有 ${result.warnings.size} 条警告"
            } else {
                "订阅更新成功"
            }
            buildUpdateResult(context, item, message, result.warnings)
        } catch (e: Exception) {
            val updatedItem = item.copy(lastError = e.message ?: "订阅更新失败")
            val state = replaceItem(context, updatedItem)
            SubscriptionUpdateResult(false, updatedItem.lastError ?: "订阅更新失败", emptyList(), state)
        }
    }

    private fun buildUpdateResult(
        context: Context,
        item: SubscriptionItem,
        message: String,
        warnings: List<String>
    ): SubscriptionUpdateResult {
        val updatedItem = item.copy(
            lastUpdatedAt = System.currentTimeMillis(),
            lastError = null
        )
        val state = replaceItem(context, updatedItem)
        return SubscriptionUpdateResult(true, message, warnings, state)
    }

    private fun replaceItem(context: Context, item: SubscriptionItem): SubscriptionState {
        val state = load(context)
        val items = state.items.map { if (it.id == item.id) item else it }
        val newState = SubscriptionState(items, state.selectedId)
        save(context, newState)
        return newState
    }

    private fun save(context: Context, state: SubscriptionState) {
        val file = File(context.filesDir, FILE_NAME)
        val root = JSONObject()
        root.put("selectedId", state.selectedId ?: "")
        val items = JSONArray()
        state.items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("url", item.url)
            obj.put("lastUpdatedAt", item.lastUpdatedAt ?: 0)
            obj.put("lastError", item.lastError ?: "")
            items.put(obj)
        }
        root.put("items", items)
        file.writeText(root.toString(2))
    }

    private fun deriveName(url: String, index: Int): String {
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        return host ?: "订阅$index"
    }

    private fun fetchSubscription(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "SingBoxMobile/1.0")
        val code = connection.responseCode
        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IOException("订阅请求失败: HTTP $code")
        }
        if (body.isBlank()) {
            throw IOException("订阅内容为空")
        }
        return decodeIfBase64(body)
    }

    private fun decodeIfBase64(raw: String): String {
        val trimmed = raw.trim()
        if (isLikelyYaml(trimmed) || isLikelyJson(trimmed)) {
            return raw
        }
        if (!looksLikeBase64(trimmed)) {
            return raw
        }
        val decoded = decodeBase64(trimmed) ?: return raw
        return if (isLikelyYaml(decoded) || isLikelyJson(decoded)) decoded else raw
    }

    private fun looksLikeBase64(text: String): Boolean {
        if (text.length < 16) {
            return false
        }
        val normalized = text.replace("\n", "").replace("\r", "")
        if (normalized.length % 4 != 0) {
            return false
        }
        val base64Regex = Regex("^[A-Za-z0-9+/=_-]+$")
        return base64Regex.matches(normalized)
    }

    private fun decodeBase64(text: String): String? {
        val normalized = text.replace("\n", "").replace("\r", "")
        return runCatching {
            val decoded = Base64.decode(normalized, Base64.NO_WRAP)
            String(decoded, Charsets.UTF_8)
        }.getOrNull() ?: runCatching {
            val decoded = Base64.decode(normalized, Base64.NO_WRAP or Base64.URL_SAFE)
            String(decoded, Charsets.UTF_8)
        }.getOrNull()
    }

    private fun isLikelyYaml(text: String): Boolean {
        val sample = text.trimStart()
        if (sample.startsWith("---")) {
            return true
        }
        val keys = listOf(
            "proxies:",
            "proxy-providers:",
            "proxy-groups:",
            "rules:",
            "dns:",
            "mixed-port:",
            "port:",
            "mode:"
        )
        return keys.any { sample.startsWith(it) || text.contains("\n$it") }
    }

    private fun isLikelyJson(text: String): Boolean {
        val sample = text.trimStart()
        if (!(sample.startsWith("{") || sample.startsWith("["))) {
            return false
        }
        return sample.contains("\"outbounds\"") || sample.contains("\"inbounds\"")
    }
}
