package com.droidremote.universal
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TabletReceiverService : Service() {
    private var server: RealReceiverServer? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("droidremote","DroidRemote REAL",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val ip = NetworkUtil.getLocalIp()
        val notif = NotificationCompat.Builder(this,"droidremote")
            .setContentTitle("DroidRemote TV actif")
            .setContentText("Controle sur $ip:8080")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(1, notif)
        server = RealReceiverServer(this, 8080)
        try { server?.start() } catch (e: Exception) { e.printStackTrace() }
    }
    override fun onDestroy() { server?.stop(); super.onDestroy() }
}
