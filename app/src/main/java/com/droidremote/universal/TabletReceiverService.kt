package com.droidremote.universal
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.Context
import android.net.wifi.WifiManager
class TabletReceiverService : Service() {
    private var server: RealReceiverServer? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("droidremote","DroidRemote REAL",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(this,"droidremote")
            .setContentTitle("DroidRemote REEL actif")
            .setContentText("Controle sur ${getIp()}:8080")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        // FIX: pas de type mediaProjection, juste startForeground normal pour Android 14
        startForeground(1, notif)
        server = RealReceiverServer(this, 8080)
        try { server?.start() } catch (e: Exception) { e.printStackTrace() }
    }
    override fun onDestroy() { server?.stop(); super.onDestroy() }
    private fun getIp(): String {
        return try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wm.connectionInfo.ipAddress
            String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        } catch (e: Exception) { "192.168.x.x" }
    }
}
