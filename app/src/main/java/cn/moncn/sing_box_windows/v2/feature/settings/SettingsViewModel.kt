package cn.moncn.sing_box_windows.v2.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.moncn.sing_box_windows.v2.data.core.CoreInfoStore
import cn.moncn.sing_box_windows.v2.domain.settings.AppSettings
import cn.moncn.sing_box_windows.v2.domain.update.UpdateState
import cn.moncn.sing_box_windows.v2.core.arch.MviViewModel
import cn.moncn.sing_box_windows.v2.domain.settings.SettingsGateway
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val gateway: SettingsGateway
) : MviViewModel<SettingsIntent, SettingsUiState, Nothing>(SettingsUiState()) {

    init {
        observeUpdateState()
        // 内核版本直接取自 libbox，无需网络请求
        updateState { current -> current.copy(kernelVersion = CoreInfoStore.refresh()) }
        submitIntent(SettingsIntent.Load)
    }

    override suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> load()
            is SettingsIntent.ChangeDnsStrategy -> updateState { it.copy(dnsStrategy = intent.value) }
            is SettingsIntent.ChangeDnsCacheEnabled -> updateState { it.copy(dnsCacheEnabled = intent.value) }
            is SettingsIntent.ChangeDnsIndependentCache -> updateState { it.copy(dnsIndependentCache = intent.value) }
            is SettingsIntent.ChangeTunMtuInput -> updateState {
                it.copy(tunMtuInput = intent.value.filter(Char::isDigit))
            }
            is SettingsIntent.ChangeTunAutoRoute -> updateState { it.copy(tunAutoRoute = intent.value) }
            is SettingsIntent.ChangeTunStrictRoute -> updateState { it.copy(tunStrictRoute = intent.value) }
            is SettingsIntent.ChangeHttpProxyEnabled -> updateState { it.copy(httpProxyEnabled = intent.value) }
            SettingsIntent.Save -> save()
            SettingsIntent.CheckUpdate -> gateway.checkUpdate(manual = true)
            SettingsIntent.DownloadUpdate -> downloadUpdate()
            SettingsIntent.InstallDownloadedUpdate -> installDownloadedUpdate()
            SettingsIntent.ClearError -> updateState { it.copy(error = null) }
            SettingsIntent.ClearMessage -> updateState { it.copy(message = null) }
        }
    }

    private fun observeUpdateState() {
        viewModelScope.launch {
            gateway.updateStateFlow.collect { update ->
                updateState { current ->
                    current.copy(
                        updateState = update
                    )
                }
            }
        }
    }

    private fun downloadUpdate() {
        val release = (state.value.updateState as? UpdateState.UpdateAvailable)?.releaseInfo
        if (release == null) {
            updateState {
                it.copy(
                    message = "当前没有可下载的新版本"
                )
            }
            return
        }
        gateway.downloadUpdate(release)
    }

    private fun installDownloadedUpdate() {
        val readyState = state.value.updateState as? UpdateState.ReadyToInstall
        if (readyState == null) {
            updateState {
                it.copy(
                    message = "安装包尚未下载完成"
                )
            }
            return
        }

        if (!gateway.canInstallPackage()) {
            gateway.openInstallPermissionSettings()
            updateState {
                it.copy(
                    message = "请先开启“允许安装未知应用”权限后再安装"
                )
            }
            return
        }

        val started = gateway.installUpdate(readyState.apkFile)
        if (!started) {
            updateState {
                it.copy(
                    error = "无法启动安装器，请稍后重试"
                )
            }
        }
    }

    private suspend fun load() {
        updateState { it.copy(isLoading = true, error = null, message = null) }
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
                        isLoading = false
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "读取设置失败"
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

        updateState { it.copy(isSaving = true, error = null, message = null) }
        runCatching { gateway.saveSettings(settings) }
            .onSuccess {
                updateState {
                    it.copy(
                        isSaving = false,
                        tunMtuInput = normalizedMtu.toString(),
                        message = "设置已保存，重新连接后生效"
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isSaving = false,
                        error = error.message ?: "保存失败"
                    )
                }
            }
    }

    private fun now(): Long = System.currentTimeMillis()


    /**
     * Intent 处理出现未预期异常时的兜底：写入错误状态，
     * 避免异常只停留在日志里、用户却看不到任何提示。
     */
    override fun onIntentError(intent: SettingsIntent, error: Throwable) {
        updateState { current -> current.copy(error = error.message ?: "操作失败") }
    }

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
