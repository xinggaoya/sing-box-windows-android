package cn.moncn.sing_box_windows.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.net.ProxyInfo
import android.net.wifi.WifiManager
import androidx.core.app.NotificationCompat
import cn.moncn.sing_box_windows.MainActivity
import cn.moncn.sing_box_windows.R
import cn.moncn.sing_box_windows.config.ConfigRepository
import cn.moncn.sing_box_windows.core.LibboxManager
import cn.moncn.sing_box_windows.core.SingBoxEngine
import io.nekohasekai.libbox.Notification as LibboxNotification
import io.nekohasekai.libbox.RoutePrefixIterator
import io.nekohasekai.libbox.TunOptions

class AppVpnService : android.net.VpnService() {
    private var vpnInterface: android.os.ParcelFileDescriptor? = null
    private val platform by lazy { PlatformInterfaceBridge(this, this) }
    val wifiManager: WifiManager? by lazy { getSystemService(WifiManager::class.java) }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        LibboxManager.initialize(this)
        DefaultNetworkMonitor.initialize(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTunnel()
            ACTION_STOP -> stopTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        if (vpnInterface != null || SingBoxEngine.isRunning()) {
            return
        }
        VpnStateStore.update(VpnState.CONNECTING)
        val notification = buildNotification("Connecting")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val configJson = ConfigRepository.loadOrCreateConfig(this)
        val result = SingBoxEngine.start(configJson, platform)
        if (!result.ok) {
            VpnStateStore.update(VpnState.ERROR, result.error)
            stopTunnel(resetState = false)
            return
        }

        VpnStateStore.update(VpnState.CONNECTED)
        notifyStatus("Connected")
    }

    private fun stopTunnel(resetState: Boolean = true) {
        SingBoxEngine.stop()
        vpnInterface?.close()
        vpnInterface = null
        if (resetState) {
            VpnStateStore.update(VpnState.IDLE)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(status: String): Notification {
        val channelId = ensureChannel()
        val intent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SingBox VPN")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun notifyStatus(status: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun ensureChannel(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            manager?.createNotificationChannel(channel)
        }
        return CHANNEL_ID
    }

    fun openTunForCore(options: TunOptions): Int {
        vpnInterface?.close()
        vpnInterface = null

        val builder = Builder()
            .setSession(VpnDefaults.SESSION_NAME)
            .setMtu(options.mtu)

        var addressCount = 0
        addressCount += addAddresses(builder, options.inet4Address)
        addressCount += addAddresses(builder, options.inet6Address)
        if (addressCount == 0) {
            builder.addAddress(VpnDefaults.ADDRESS, VpnDefaults.PREFIX)
        }

        val dnsAddress = runCatching { options.dnsServerAddress?.value }.getOrNull()
        if (!dnsAddress.isNullOrBlank()) {
            builder.addDnsServer(dnsAddress)
        } else {
            builder.addDnsServer(VpnDefaults.DNS)
        }

        var routeCount = 0
        routeCount += addRoutes(builder, options.inet4RouteRange)
        routeCount += addRoutes(builder, options.inet6RouteRange)
        if (routeCount == 0) {
            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)
        }

        applyPackageRules(builder, options)
        applyHttpProxy(builder, options)

        vpnInterface = builder.establish()
        if (vpnInterface == null) {
            error("vpn establish failed")
        }

        return vpnInterface?.fd ?: error("tun fd unavailable")
    }

    fun notifyCoreNotification(notification: LibboxNotification) {
        Log.i("Libbox", "notification: ${notification.title} ${notification.body}")
    }

    private fun addAddresses(builder: Builder, iterator: RoutePrefixIterator): Int {
        var count = 0
        while (iterator.hasNext()) {
            val prefix = iterator.next()
            builder.addAddress(prefix.address(), prefix.prefix())
            count++
        }
        return count
    }

    private fun addRoutes(builder: Builder, iterator: RoutePrefixIterator): Int {
        var count = 0
        while (iterator.hasNext()) {
            val prefix = iterator.next()
            builder.addRoute(prefix.address(), prefix.prefix())
            count++
        }
        return count
    }

    private fun applyPackageRules(builder: Builder, options: TunOptions) {
        val include = options.includePackage
        if (include.hasNext()) {
            while (include.hasNext()) {
                runCatching { builder.addAllowedApplication(include.next()) }
                    .onFailure { Log.w("AppVpnService", "allow app failed", it) }
            }
            return
        }

        val exclude = options.excludePackage
        while (exclude.hasNext()) {
            runCatching { builder.addDisallowedApplication(exclude.next()) }
                .onFailure { Log.w("AppVpnService", "disallow app failed", it) }
        }
    }

    private fun applyHttpProxy(builder: Builder, options: TunOptions) {
        if (!options.isHTTPProxyEnabled) {
            return
        }
        val host = options.httpProxyServer
        val port = options.httpProxyServerPort
        if (host.isNullOrBlank() || port <= 0) {
            return
        }

        val bypass = mutableListOf<String>()
        val bypassIterator = options.httpProxyBypassDomain
        while (bypassIterator.hasNext()) {
            bypass.add(bypassIterator.next())
        }

        val proxy = ProxyInfo.buildDirectProxy(host, port, bypass)
        builder.setHttpProxy(proxy)
    }

    companion object {
        const val ACTION_START = "cn.moncn.sing_box_windows.vpn.ACTION_START"
        const val ACTION_STOP = "cn.moncn.sing_box_windows.vpn.ACTION_STOP"
        private const val CHANNEL_ID = "singbox_vpn"
        private const val NOTIFICATION_ID = 2001
    }
}
