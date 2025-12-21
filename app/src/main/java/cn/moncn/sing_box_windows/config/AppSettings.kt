package cn.moncn.sing_box_windows.config

data class AppSettings(
    val logLevel: String = AppSettingsDefaults.LOG_LEVEL,
    val logTimestamp: Boolean = AppSettingsDefaults.LOG_TIMESTAMP,
    val dnsStrategy: String = AppSettingsDefaults.DNS_STRATEGY,
    val dnsCacheEnabled: Boolean = AppSettingsDefaults.DNS_CACHE_ENABLED,
    val dnsIndependentCache: Boolean = AppSettingsDefaults.DNS_INDEPENDENT_CACHE,
    val tunMtu: Int = AppSettingsDefaults.TUN_MTU,
    val tunAutoRoute: Boolean = AppSettingsDefaults.TUN_AUTO_ROUTE,
    val tunStrictRoute: Boolean = AppSettingsDefaults.TUN_STRICT_ROUTE,
    val httpProxyEnabled: Boolean = AppSettingsDefaults.HTTP_PROXY_ENABLED,
    val mixedInboundPort: Int = AppSettingsDefaults.MIXED_PORT,
    val socksInboundPort: Int = AppSettingsDefaults.SOCKS_PORT,
    val clashApiEnabled: Boolean = AppSettingsDefaults.CLASH_API_ENABLED,
    val clashApiAddress: String = AppSettingsDefaults.CLASH_API_ADDRESS,
    val cacheFileEnabled: Boolean = AppSettingsDefaults.CACHE_FILE_ENABLED
)

object AppSettingsDefaults {
    const val LOG_LEVEL = "info"
    const val LOG_TIMESTAMP = true
    const val DNS_STRATEGY = "prefer_ipv4"
    const val DNS_CACHE_ENABLED = true
    const val DNS_INDEPENDENT_CACHE = true
    const val TUN_MTU = 9000
    const val TUN_AUTO_ROUTE = true
    const val TUN_STRICT_ROUTE = true
    const val HTTP_PROXY_ENABLED = true
    const val MIXED_PORT = 1082
    const val SOCKS_PORT = 7888
    const val CLASH_API_ENABLED = true
    const val CLASH_API_ADDRESS = "127.0.0.1:9090"
    const val CACHE_FILE_ENABLED = true
}
