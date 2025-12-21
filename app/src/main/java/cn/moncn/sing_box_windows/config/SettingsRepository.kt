package cn.moncn.sing_box_windows.config

import android.content.Context

object SettingsRepository {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LOG_LEVEL = "log_level"
    private const val KEY_LOG_TIMESTAMP = "log_timestamp"
    private const val KEY_DNS_STRATEGY = "dns_strategy"
    private const val KEY_DNS_CACHE_ENABLED = "dns_cache_enabled"
    private const val KEY_DNS_INDEPENDENT_CACHE = "dns_independent_cache"
    private const val KEY_TUN_MTU = "tun_mtu"
    private const val KEY_TUN_AUTO_ROUTE = "tun_auto_route"
    private const val KEY_TUN_STRICT_ROUTE = "tun_strict_route"
    private const val KEY_HTTP_PROXY_ENABLED = "http_proxy_enabled"
    private const val KEY_MIXED_PORT = "mixed_port"
    private const val KEY_SOCKS_PORT = "socks_port"
    private const val KEY_CLASH_API_ENABLED = "clash_api_enabled"
    private const val KEY_CLASH_API_ADDRESS = "clash_api_address"
    private const val KEY_CACHE_FILE_ENABLED = "cache_file_enabled"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppSettings(
            logLevel = prefs.getString(KEY_LOG_LEVEL, AppSettingsDefaults.LOG_LEVEL)
                ?: AppSettingsDefaults.LOG_LEVEL,
            logTimestamp = prefs.getBoolean(KEY_LOG_TIMESTAMP, AppSettingsDefaults.LOG_TIMESTAMP),
            dnsStrategy = prefs.getString(KEY_DNS_STRATEGY, AppSettingsDefaults.DNS_STRATEGY)
                ?: AppSettingsDefaults.DNS_STRATEGY,
            dnsCacheEnabled = prefs.getBoolean(
                KEY_DNS_CACHE_ENABLED,
                AppSettingsDefaults.DNS_CACHE_ENABLED
            ),
            dnsIndependentCache = prefs.getBoolean(
                KEY_DNS_INDEPENDENT_CACHE,
                AppSettingsDefaults.DNS_INDEPENDENT_CACHE
            ),
            tunMtu = prefs.getInt(KEY_TUN_MTU, AppSettingsDefaults.TUN_MTU),
            tunAutoRoute = prefs.getBoolean(KEY_TUN_AUTO_ROUTE, AppSettingsDefaults.TUN_AUTO_ROUTE),
            tunStrictRoute = prefs.getBoolean(KEY_TUN_STRICT_ROUTE, AppSettingsDefaults.TUN_STRICT_ROUTE),
            httpProxyEnabled = prefs.getBoolean(
                KEY_HTTP_PROXY_ENABLED,
                AppSettingsDefaults.HTTP_PROXY_ENABLED
            ),
            mixedInboundPort = prefs.getInt(KEY_MIXED_PORT, AppSettingsDefaults.MIXED_PORT),
            socksInboundPort = prefs.getInt(KEY_SOCKS_PORT, AppSettingsDefaults.SOCKS_PORT),
            clashApiEnabled = prefs.getBoolean(
                KEY_CLASH_API_ENABLED,
                AppSettingsDefaults.CLASH_API_ENABLED
            ),
            clashApiAddress = prefs.getString(
                KEY_CLASH_API_ADDRESS,
                AppSettingsDefaults.CLASH_API_ADDRESS
            ) ?: AppSettingsDefaults.CLASH_API_ADDRESS,
            cacheFileEnabled = prefs.getBoolean(
                KEY_CACHE_FILE_ENABLED,
                AppSettingsDefaults.CACHE_FILE_ENABLED
            )
        )
    }

    fun save(context: Context, settings: AppSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LOG_LEVEL, settings.logLevel)
            .putBoolean(KEY_LOG_TIMESTAMP, settings.logTimestamp)
            .putString(KEY_DNS_STRATEGY, settings.dnsStrategy)
            .putBoolean(KEY_DNS_CACHE_ENABLED, settings.dnsCacheEnabled)
            .putBoolean(KEY_DNS_INDEPENDENT_CACHE, settings.dnsIndependentCache)
            .putInt(KEY_TUN_MTU, settings.tunMtu)
            .putBoolean(KEY_TUN_AUTO_ROUTE, settings.tunAutoRoute)
            .putBoolean(KEY_TUN_STRICT_ROUTE, settings.tunStrictRoute)
            .putBoolean(KEY_HTTP_PROXY_ENABLED, settings.httpProxyEnabled)
            .putInt(KEY_MIXED_PORT, settings.mixedInboundPort)
            .putInt(KEY_SOCKS_PORT, settings.socksInboundPort)
            .putBoolean(KEY_CLASH_API_ENABLED, settings.clashApiEnabled)
            .putString(KEY_CLASH_API_ADDRESS, settings.clashApiAddress)
            .putBoolean(KEY_CACHE_FILE_ENABLED, settings.cacheFileEnabled)
            .apply()
    }
}
