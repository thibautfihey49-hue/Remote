package com.droidremote.universal
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.Socket
class NetworkScanner(private val context: Context? = null) {
    private val _devices = MutableStateFlow<List<AndroidDevice>>(emptyList())
    val devices: StateFlow<List<AndroidDevice>> = _devices
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    private val foundDevices = mutableMapOf<String, AndroidDevice>()
    private var nsdManager: NsdManager? = null
    suspend fun scanNetwork() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        foundDevices.clear()
        _devices.value = emptyList()
        val baseIp = getBaseIp()
        startNsdDiscovery()
        val jobs = mutableListOf<Job>()
        for (i in 1..254) {
            val ip = "$baseIp$i"
            val job = launch { checkDevice(ip) }
            jobs.add(job)
            if (jobs.size >= 50) { jobs.forEach { it.join() }; jobs.clear() }
        }
        jobs.forEach { it.join() }
        delay(2500)
        stopNsdDiscovery()
        _devices.value = foundDevices.values.toList().sortedBy { it.ip }
        _isScanning.value = false
    }
    private suspend fun checkDevice(ip: String) {
        val isOurReceiver = isPortOpen(ip, 8080, 300)
        val isChromecast = isPortOpen(ip, 8009, 300) || isPortOpen(ip, 8008, 300)
        val isAndroidTvRemote = isPortOpen(ip, 6466, 300) || isPortOpen(ip, 6467, 300)
        if (isOurReceiver) addDevice(AndroidDevice(ip, "Tablette DroidRemote $ip", ip, DeviceType.TABLET, "Receiver 8080 SANS ADB", 100, true, true))
        else if (isAndroidTvRemote) addDevice(AndroidDevice(ip, "Android TV $ip", ip, DeviceType.TV, "Port 6466 SANS ADB", 100, true, false))
        else if (isChromecast) addDevice(AndroidDevice(ip, "BBoxTV / Chromecast $ip", ip, DeviceType.CHROMECAST, "Cast SANS ADB", 100, true, false))
    }
    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try { val s = Socket(); s.connect(InetSocketAddress(ip, port), timeout); s.close(); true } catch (e: Exception) { false }
    }
    private fun getBaseIp(): String {
        return try {
            val wm = context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            val ip = String.format("%d.%d.%d.", ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff)
            if (ip == "0.0.0.") "192.168.1." else ip
        } catch (e: Exception) { "192.168.1." }
    }
    private fun startNsdDiscovery() {
        try {
            nsdManager = context?.getSystemService(Context.NSD_SERVICE) as? NsdManager
            nsdManager?.discoverServices("_googlecast._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            nsdManager?.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {}
    }
    private fun stopNsdDiscovery() { try { nsdManager?.stopServiceDiscovery(discoveryListener) } catch (e: Exception) {} }
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) { nsdManager?.resolveService(service, resolveListener) }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            val ip = serviceInfo.host?.hostAddress ?: return
            val name = serviceInfo.serviceName
            val type = if (serviceInfo.serviceType.contains("googlecast")) DeviceType.CHROMECAST else DeviceType.TV
            addDevice(AndroidDevice(ip, name, ip, type, "mDNS SANS ADB", 100, true, false))
        }
    }
    private fun addDevice(device: AndroidDevice) {
        if (!foundDevices.containsKey(device.ip)) { foundDevices[device.ip] = device; _devices.value = foundDevices.values.toList() }
    }
    fun sendCommand(device: AndroidDevice, command: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (device.isReceiverInstalled) {
                    val url = if(command.startsWith("input text")) {
                        val txt = command.removePrefix("input text ").trim()
                        "http://${device.ip}:8080/text?text=${java.net.URLEncoder.encode(txt, "UTF-8")}"
                    } else "http://${device.ip}:8080/command?cmd=$command"
                    (java.net.URL(url).openConnection() as java.net.HttpURLConnection).responseCode
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
