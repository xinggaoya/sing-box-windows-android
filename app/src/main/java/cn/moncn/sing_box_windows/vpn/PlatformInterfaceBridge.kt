package cn.moncn.sing_box_windows.vpn

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build
import android.os.Process
import android.system.OsConstants
import android.util.Log
import androidx.annotation.RequiresApi
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterface as LibboxNetworkInterface
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

/**
 * Android 平台的 libbox 接口桥接。
 *
 * 适配 sing-box 1.14 的 `PlatformInterface`：
 * 新增的 Shell / SSH / Bridge / Tailscale / 邻居发现等能力在 Android 上不适用，
 * 统一以「明确不支持」的方式返回，避免内核误判为可用。
 */
class PlatformInterfaceBridge(
    private val context: Context,
    private val vpnService: AppVpnService
) : PlatformInterface {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        vpnService.protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        return vpnService.openTunForCore(options)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int
    ): ConnectionOwner {
        val uid = connectivity?.getConnectionOwnerUid(
            ipProtocol,
            InetSocketAddress(sourceAddress, sourcePort),
            InetSocketAddress(destinationAddress, destinationPort)
        ) ?: Process.INVALID_UID
        if (uid == Process.INVALID_UID) {
            error("android: connection owner not found")
        }
        val owner = ConnectionOwner()
        owner.userId = uid
        val packages = context.packageManager.getPackagesForUid(uid)
        if (!packages.isNullOrEmpty()) {
            owner.userName = packages[0]
            owner.processPath = runCatching {
                context.packageManager.getApplicationInfo(packages[0], 0).sourceDir
            }.getOrNull() ?: ""
            owner.setAndroidPackageNames(StringArray(packages.toList()))
        }
        return owner
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        DefaultNetworkMonitor.setListener(null)
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val networks = connectivity?.allNetworksCompat() ?: emptyArray()
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
        val interfaces = mutableListOf<LibboxNetworkInterface>()
        for (network in networks) {
            val boxInterface = LibboxNetworkInterface()
            val linkProperties = connectivity?.getLinkProperties(network) ?: continue
            val networkCapabilities = connectivity?.getNetworkCapabilities(network) ?: continue
            boxInterface.name = linkProperties.interfaceName
            val networkInterface =
                networkInterfaces.find { it.name == boxInterface.name } ?: continue
            boxInterface.dnsServer =
                StringArray(linkProperties.dnsServers.mapNotNull { it.hostAddress })
            boxInterface.type = when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                else -> Libbox.InterfaceTypeOther
            }
            boxInterface.index = networkInterface.index
            runCatching { boxInterface.mtu = networkInterface.mtu }.onFailure {
                Log.e("PlatformInterface", "get mtu failed: ${boxInterface.name}", it)
            }
            boxInterface.addresses =
                StringArray(networkInterface.interfaceAddresses.mapTo(mutableListOf()) { it.toPrefix() })
            var flags = 0
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                flags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
            }
            if (networkInterface.isLoopback) flags = flags or OsConstants.IFF_LOOPBACK
            if (networkInterface.isPointToPoint) flags = flags or OsConstants.IFF_POINTOPOINT
            if (networkInterface.supportsMulticast()) flags = flags or OsConstants.IFF_MULTICAST
            boxInterface.flags = flags
            boxInterface.metered =
                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            interfaces.add(boxInterface)
        }
        return InterfaceArray(interfaces.iterator())
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun clearDNSCache() = Unit

    override fun readWIFIState(): WIFIState? {
        val wifiInfo = resolveWifiInfo() ?: return null
        var ssid = wifiInfo.ssid
        if (ssid == "<unknown ssid>") {
            return WIFIState("", "")
        }
        if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length - 1)
        }
        return WIFIState(ssid, wifiInfo.bssid)
    }

    override fun localDNSTransport(): LocalDNSTransport? = LocalResolver

    override fun sendNotification(notification: io.nekohasekai.libbox.Notification) {
        vpnService.notifyCoreNotification(notification)
    }

    override fun cancelNotification(identifier: String, typeID: Int) {
        vpnService.cancelCoreNotification(identifier, typeID)
    }

    // ==================== Android 不支持的平台能力 ====================

    override fun usePlatformShell(): Boolean = false

    override fun checkPlatformShell() {
        throw UnsupportedOperationException("platform shell is not supported on Android")
    }

    override fun openShellSession(
        user: PlatformUser?,
        command: String?,
        environ: StringIterator?,
        term: String?,
        rows: Int,
        cols: Int
    ): ShellSession {
        throw UnsupportedOperationException("platform shell is not supported on Android")
    }

    override fun lookupUser(username: String?): PlatformUser {
        throw UnsupportedOperationException("lookupUser is not supported on Android")
    }

    override fun lookupSFTPServer(): String {
        throw UnsupportedOperationException("lookupSFTPServer is not supported on Android")
    }

    override fun readSystemSSHHostKey(): String {
        throw UnsupportedOperationException("readSystemSSHHostKey is not supported on Android")
    }

    override fun usePlatformBridge(): Boolean = false

    override fun createBridge(options: BridgeOptions?): BridgeSession {
        throw UnsupportedOperationException("platform bridge is not supported on Android")
    }

    override fun tailscaleHostname(): String = ""

    override fun registerMyInterface(name: String?) = Unit

    override fun startNeighborMonitor(listener: NeighborUpdateListener) = Unit

    override fun closeNeighborMonitor(listener: NeighborUpdateListener) = Unit

    private class InterfaceArray(
        private val iterator: Iterator<LibboxNetworkInterface>
    ) : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): LibboxNetworkInterface = iterator.next()
    }

    private class StringArray(items: List<String>) : StringIterator {
        private val list = items
        private var index = 0

        override fun len(): Int = list.size
        override fun hasNext(): Boolean = index < list.size
        override fun next(): String = list[index++]
    }

    private fun InterfaceAddress.toPrefix(): String {
        return if (address is Inet6Address) {
            "${Inet6Address.getByAddress(address.address).hostAddress}/${networkPrefixLength}"
        } else {
            "${address.hostAddress}/${networkPrefixLength}"
        }
    }

    private fun resolveWifiInfo(): WifiInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val active = connectivity?.activeNetwork ?: return null
            val caps = connectivity.getNetworkCapabilities(active) ?: return null
            return caps.transportInfo as? WifiInfo
        }
        @Suppress("DEPRECATION")
        return vpnService.wifiManager?.connectionInfo
    }
}
