package cn.moncn.sing_box_windows.v2.data.core

import android.content.Context
import cn.moncn.sing_box_windows.v2.data.config.ConfigRepository
import cn.moncn.sing_box_windows.v2.data.config.ConfigSettingsApplier
import cn.moncn.sing_box_windows.v2.data.config.SettingsRepository

/**
 * 统一核心运行时协调器：
 *
 * 1) 启动 VPN 前准备并持久化应用后的配置；
 * 2) 根据连接状态管理内核命令通道（`CommandClient`）的生命周期；
 * 3) 连接建立后把用户偏好的代理模式同步给内核。
 *
 * 原实现依赖 Clash API（HTTP 9090 + WebSocket），现已改为 libbox 自带的
 * `CommandServer` / `CommandClient` 原生通道。
 */
object CoreRuntimeCoordinator {

    /** 生成应用了用户设置的运行配置 */
    suspend fun prepareRuntimeConfig(context: Context): String {
        val rawConfig = ConfigRepository.loadOrCreateConfig(context)
        val settings = SettingsRepository.load(context)
        val appliedConfig = ConfigSettingsApplier.applySettings(rawConfig, settings)
        ConfigRepository.saveConfig(context, appliedConfig)
        return appliedConfig
    }

    /**
     * 同步内核命令通道状态。
     *
     * @param connected 内核是否已启动
     */
    suspend fun syncByConnectionState(context: Context, connected: Boolean) {
        if (!connected) {
            CoreCommandBridge.disconnect()
            return
        }

        CoreCommandBridge.connect()

        // 代理模式不再写进配置，改为连接后通过内核命令通道下发
        val settings = SettingsRepository.load(context)
        if (settings.clashMode.isNotBlank()) {
            CoreCommandBridge.gateway.setClashMode(settings.clashMode)
        }
    }
}
