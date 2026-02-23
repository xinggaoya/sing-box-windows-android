package cn.moncn.sing_box_windows.core

import android.content.Context
import cn.moncn.sing_box_windows.config.ClashApiDefaults
import cn.moncn.sing_box_windows.config.ConfigRepository
import cn.moncn.sing_box_windows.config.ConfigSettingsApplier
import cn.moncn.sing_box_windows.config.SettingsRepository

/**
 * 统一核心运行时协调器：
 * 1) 启动 VPN 前准备并持久化应用后的配置
 * 2) 根据连接状态管理 Clash API 与各类 Manager 生命周期
 */
object CoreRuntimeCoordinator {
    suspend fun prepareRuntimeConfig(context: Context): String {
        val rawConfig = ConfigRepository.loadOrCreateConfig(context)
        val settings = SettingsRepository.load(context)
        ClashModeManager.updatePreferredMode(resolvePreferredMode(settings.clashMode))
        val appliedConfig = ConfigSettingsApplier.applySettings(rawConfig, settings)
        ConfigRepository.saveConfig(context, appliedConfig)
        return appliedConfig
    }

    suspend fun syncByConnectionState(context: Context, connected: Boolean) {
        if (!connected) {
            stopManagersAndReset()
            return
        }

        val rawConfig = ConfigRepository.loadOrCreateConfig(context)
        val settings = SettingsRepository.load(context)
        ClashModeManager.updatePreferredMode(resolvePreferredMode(settings.clashMode))
        val appliedConfig = ConfigSettingsApplier.applySettings(rawConfig, settings)
        ClashApiClient.configureFromConfig(appliedConfig)
        if (!ClashApiClient.isConfigured()) {
            ClashApiClient.configure(ClashApiDefaults.ADDRESS, ClashApiDefaults.SECRET)
        }
        CoreStatusManager.start()
        OutboundGroupManager.start()
        CoreInfoManager.start()
        ClashModeManager.start()
    }

    private fun stopManagersAndReset() {
        ClashModeManager.stop()
        OutboundGroupManager.stop()
        CoreStatusManager.stop()
        CoreInfoManager.stop()
        ClashApiClient.reset()
    }

    private fun resolvePreferredMode(value: String): ClashModeManager.ClashMode {
        return ClashModeManager.ClashMode.fromValue(value) ?: ClashModeManager.ClashMode.Rule
    }
}
