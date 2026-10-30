package cn.moncn.sing_box_windows.v2.data.core

import android.content.Context
import cn.moncn.sing_box_windows.v2.data.config.ConfigRepository
import cn.moncn.sing_box_windows.v2.data.config.ConfigSettingsApplier
import cn.moncn.sing_box_windows.v2.data.config.SettingsRepository
import cn.moncn.sing_box_windows.v2.domain.core.CoreClashMode

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

    /**
     * 生成应用了用户设置的运行配置。
     *
     * 注意：磁盘上保存的始终是**模板态**配置（保留 `clash_mode` 规则），
     * 只有在真正启动内核时才按当前代理模式把 `clash_mode` 规则固化。
     * 否则一旦固化，规则就消失，后续无法再切换模式。
     */
    suspend fun prepareRuntimeConfig(context: Context): String {
        val rawConfig = ConfigRepository.loadOrCreateConfig(context)
        val settings = SettingsRepository.load(context)
        val appliedConfig = ConfigSettingsApplier.applySettings(rawConfig, settings)
        ConfigRepository.saveConfig(context, appliedConfig)
        val mode = CoreClashMode.fromRaw(settings.clashMode) ?: CoreClashMode.RULE
        return ConfigSettingsApplier.applyClashMode(appliedConfig, mode)
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
    }

    /**
     * 重新生成配置并重载内核，使代理模式等配置变更立即生效。
     *
     * 本项目的代理模式通过 `route.rules[].clash_mode` 表达（未开启
     * `experimental.clash_api`），内核命令通道的 `SetClashMode` 不可用，
     * 因此只能改配置后重载内核。
     */
    suspend fun reloadCore(context: Context): Result<Unit> = runCatching {
        val appliedConfig = prepareRuntimeConfig(context)
        SingBoxEngine.reload(appliedConfig).getOrThrow()
    }
}
