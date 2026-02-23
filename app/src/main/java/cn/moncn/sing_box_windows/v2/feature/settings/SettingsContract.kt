package cn.moncn.sing_box_windows.v2.feature.settings

import cn.moncn.sing_box_windows.config.AppSettingsDefaults
import cn.moncn.sing_box_windows.update.UpdateState
import cn.moncn.sing_box_windows.v2.core.arch.UiIntent
import cn.moncn.sing_box_windows.v2.core.arch.UiState

data class SettingsUiState(
    val dnsStrategy: String = AppSettingsDefaults.DNS_STRATEGY,
    val dnsCacheEnabled: Boolean = AppSettingsDefaults.DNS_CACHE_ENABLED,
    val dnsIndependentCache: Boolean = AppSettingsDefaults.DNS_INDEPENDENT_CACHE,
    val tunMtuInput: String = AppSettingsDefaults.TUN_MTU.toString(),
    val tunAutoRoute: Boolean = AppSettingsDefaults.TUN_AUTO_ROUTE,
    val tunStrictRoute: Boolean = AppSettingsDefaults.TUN_STRICT_ROUTE,
    val httpProxyEnabled: Boolean = AppSettingsDefaults.HTTP_PROXY_ENABLED,
    val clashMode: String = AppSettingsDefaults.CLASH_MODE,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val updateState: UpdateState = UpdateState.Idle,
    val message: String? = null,
    val error: String? = null,
    val updatedAt: Long = 0L
) : UiState

sealed interface SettingsIntent : UiIntent {
    data object Load : SettingsIntent
    data class ChangeDnsStrategy(val value: String) : SettingsIntent
    data class ChangeDnsCacheEnabled(val value: Boolean) : SettingsIntent
    data class ChangeDnsIndependentCache(val value: Boolean) : SettingsIntent
    data class ChangeTunMtuInput(val value: String) : SettingsIntent
    data class ChangeTunAutoRoute(val value: Boolean) : SettingsIntent
    data class ChangeTunStrictRoute(val value: Boolean) : SettingsIntent
    data class ChangeHttpProxyEnabled(val value: Boolean) : SettingsIntent
    data object Save : SettingsIntent
    data object CheckUpdate : SettingsIntent
    data object DownloadUpdate : SettingsIntent
    data object InstallDownloadedUpdate : SettingsIntent
    data object ClearMessage : SettingsIntent
    data object ClearError : SettingsIntent
}
