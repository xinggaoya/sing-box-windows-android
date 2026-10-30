package cn.moncn.sing_box_windows.v2.domain.subscription

/**
 * 订阅地址归一化。
 *
 * 部分输入法即使已声明 `KeyboardCapitalization.None`，仍会把 URL 的协议头首字母
 * 大写成 `Https://`（或把路径中的域名首字母大写），导致订阅地址直接失效、
 * 表现为「订阅添加成功但同步一直失败」。
 *
 * 这里在入库前统一把**协议头**小写化；路径、参数与锚点保持原样
 * （它们大小写敏感，不能盲目 lowerCase）。
 */
object SubscriptionUrl {

    private val SCHEME_PATTERN = Regex("^([A-Za-z][A-Za-z0-9+.\\-]*)://")

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val match = SCHEME_PATTERN.find(trimmed) ?: return trimmed
        val scheme = match.groupValues[1]
        val rest = trimmed.substring(scheme.length)
        return scheme.lowercase() + rest
    }
}
