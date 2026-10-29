package cn.moncn.sing_box_windows.v2.data.core

import android.util.Log
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.SystemProxyStatus

/**
 * 内核运行时引擎。
 *
 * sing-box 1.11 起 libbox 不再暴露独立的 `BoxService`，
 * 内核实例由 `CommandServer` 托管：先 `start()` 拉起本地命令服务（gRPC），
 * 再 `startOrReloadService()` 启动 / 重载内核。
 *
 * 该命令服务正是桌面端 `sing-box-windows` 所使用的 sing-box 官方 API 的同一实现，
 * 应用侧通过 `CommandClient` 订阅即可获得流量 / 连接 / 日志 / 代理组的实时推送。
 */
object SingBoxEngine {
    private const val TAG = "SingBoxEngine"

    @Volatile
    private var server: CommandServer? = null

    @Volatile
    private var callbacks: CoreServerCallbacks? = null

    val isRunning: Boolean get() = server != null

    /**
     * 启动内核。
     *
     * @param configJson 已应用用户设置后的完整 sing-box 配置
     * @param platform 平台桥接（提供 TUN / 网络信息等）
     * @param callbacks 内核回调
     */
    fun start(
        configJson: String,
        platform: PlatformInterface,
        callbacks: CoreServerCallbacks
    ): CoreStartResult {
        if (server != null) {
            return CoreStartResult(ok = true)
        }
        this.callbacks = callbacks
        var instance: CommandServer? = null
        return try {
            instance = Libbox.newCommandServer(handler, platform)
            // 拉起本地命令服务（未配置端口时使用 basePath 下的 unix socket）
            instance.start()
            // 启动内核
            instance.startOrReloadService(configJson, OverrideOptions())
            server = instance
            CoreStartResult(ok = true)
        } catch (error: Exception) {
            Log.e(TAG, "core start failed", error)
            // 启动失败时必须关闭已拉起的命令服务，否则 gRPC Server 与 socket 会残留泄漏
            runCatching { instance?.closeService() }
                .onFailure { Log.w(TAG, "close service failed", it) }
            runCatching { instance?.close() }
                .onFailure { Log.w(TAG, "close command server failed", it) }
            server = null
            this.callbacks = null
            CoreStartResult(ok = false, error = error.message ?: "core start failed")
        }
    }

    /** 停止内核并释放命令服务 */
    fun stop() {
        release()
    }

    /**
     * 使用新配置重载内核。
     * 供 `CommandServerHandler.serviceReload()` 与设置变更使用。
     */
    fun reload(configJson: String): Result<Unit> {
        val instance = server ?: return Result.failure(IllegalStateException("内核未运行"))
        return runCatching { instance.startOrReloadService(configJson, OverrideOptions()) }
    }

    private fun release() {
        val instance = server
        server = null
        callbacks = null
        if (instance == null) return
        runCatching { instance.closeService() }
            .onFailure { Log.w(TAG, "close service failed", it) }
        runCatching { instance.close() }
            .onFailure { Log.w(TAG, "close command server failed", it) }
    }

    private val handler = object : CommandServerHandler {
        override fun serviceStop() {
            callbacks?.onStopRequested()
        }

        override fun serviceReload() {
            val config = callbacks?.onReloadRequested() ?: return
            val result = reload(config)
            if (result.isFailure) {
                Log.e(TAG, "service reload failed", result.exceptionOrNull())
            }
        }

        /** Android 端不提供系统代理能力 */
        override fun getSystemProxyStatus(): SystemProxyStatus {
            return SystemProxyStatus().apply {
                available = false
                enabled = false
            }
        }

        /** Android 端不提供系统代理能力 */
        override fun setSystemProxyEnabled(isEnabled: Boolean) {
            Log.i(TAG, "setSystemProxyEnabled ignored on Android: $isEnabled")
        }

        /** 触发原生崩溃用于定位内核问题，App 侧不开放 */
        override fun triggerNativeCrash() {
            throw UnsupportedOperationException("triggerNativeCrash is not supported")
        }

        override fun writeDebugMessage(message: String) {
            Log.d(TAG, "core: $message")
        }

        /** Android 端不提供 SSH Agent */
        override fun connectSSHAgent(): Int {
            throw UnsupportedOperationException("connectSSHAgent is not supported")
        }
    }
}
