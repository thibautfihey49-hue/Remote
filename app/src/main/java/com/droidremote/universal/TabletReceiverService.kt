package com.droidremote.universal
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.NetworkInterface
import java.net.Inet4Address

class TabletReceiverService : Service() {
    private var server: RealReceiverServer? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("droidremote","DroidRemote",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val ip = getIp()
        val notif = NotificationCompat.Builder(this,"droidremote")
            .setContentTitle("DroidRemote Tablette actif")
            .setContentText("Controle sur $ip:8080")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notif)
        server = RealReceiverServer(this, 8080)
        try { server?.start() } catch (e: Exception) { e.printStackTrace() }
    }
    override fun onDestroy() { server?.stop(); super.onDestroy() }
    private fun getIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) return ip
                    }
                }
            }
        } catch (e: Exception) {}
        return "192.168.1.x"
    }
}
