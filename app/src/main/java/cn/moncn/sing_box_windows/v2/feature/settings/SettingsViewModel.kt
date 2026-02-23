package cn.moncn.sing_box_windows.v2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.moncn.sing_box_windows.config.AppSettings
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway

class SettingsViewModel(
    private val gateway: SettingsGateway
) : MviViewModel<SettingsIntent, SettingsUiState, Nothing>(SettingsUiState()) {

    init {
        submitIntent(SettingsIntent.Load)
    }

    override suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> load()
            is SettingsIntent.ChangeDnsStrategy -> updateState { it.copy(dnsStrategy = intent.value, updatedAt = now()) }
            is SettingsIntent.ChangeDnsCacheEnabled -> updateState { it.copy(dnsCacheEnabled = intent.value, updatedAt = now()) }
            is SettingsIntent.ChangeDnsIndependentCache -> updateState { it.copy(dnsIndependentCache = intent.value, updatedAt = now()) }
            is SettingsIntent.ChangeTunMtuInput -> updateState {
                it.copy(tunMtuInput = intent.value.filter(Char::isDigit), updatedAt = now())
            }
            is SettingsIntent.ChangeTunAutoRoute -> updateState { it.copy(tunAutoRoute = intent.value, updatedAt = now()) }
            is SettingsIntent.ChangeTunStrictRoute -> updateState { it.copy(tunStrictRoute = intent.value, updatedAt = now()) }
            is SettingsIntent.ChangeHttpProxyEnabled -> updateState { it.copy(httpProxyEnabled = intent.value, updatedAt = now()) }
            SettingsIntent.Save -> save()
            SettingsIntent.CheckUpdate -> gateway.checkUpdate(manual = true)
            SettingsIntent.ClearError -> updateState { it.copy(error = null, updatedAt = now()) }
            SettingsIntent.ClearMessage -> updateState { it.copy(message = null, updatedAt = now()) }
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, error = null, message = null, updatedAt = now()) }
        runCatching { gateway.loadSettings() }
            .onSuccess { settings ->
                updateState {
                    it.copy(
                        dnsStrategy = settings.dnsStrategy,
                        dnsCacheEnabled = settings.dnsCacheEnabled,
                        dnsIndependentCache = settings.dnsIndependentCache,
                        tunMtuInput = settings.tunMtu.toString(),
                        tunAutoRoute = settings.tunAutoRoute,
                        tunStrictRoute = settings.tunStrictRoute,
                        httpProxyEnabled = settings.httpProxyEnabled,
                        clashMode = settings.clashMode,
                        isLoading = false,
                        updatedAt = now()
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "读取设置失败",
                        updatedAt = now()
                    )
                }
            }
    }

    private suspend fun save() {
        val current = state.value
        if (current.isLoading || current.isSaving) return

        val normalizedMtu = current.tunMtuInput.toIntOrNull()?.coerceIn(1280, 9000) ?: 9000
        val settings = AppSettings(
            dnsStrategy = current.dnsStrategy,
            dnsCacheEnabled = current.dnsCacheEnabled,
            dnsIndependentCache = current.dnsIndependentCache,
            tunMtu = normalizedMtu,
            tunAutoRoute = current.tunAutoRoute,
            tunStrictRoute = current.tunStrictRoute,
            httpProxyEnabled = current.httpProxyEnabled,
            clashMode = current.clashMode
        )

        updateState { it.copy(isSaving = true, error = null, message = null, updatedAt = now()) }
        runCatching { gateway.saveSettings(settings) }
            .onSuccess {
                updateState {
                    it.copy(
                        isSaving = false,
                        tunMtuInput = normalizedMtu.toString(),
                        message = "设置已保存，重新连接后生效",
                        updatedAt = now()
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "保存失败",
                        updatedAt = now()
                    )
                }
            }
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        fun factory(gateway: SettingsGateway): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(gateway) as T
                }
            }
        }
    }
}
