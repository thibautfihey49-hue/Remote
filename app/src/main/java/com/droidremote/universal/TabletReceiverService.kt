package com.droidremote.universal
import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
class TabletReceiverService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("droidremote","DroidRemote",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notif = NotificationCompat.Builder(this,"droidremote").setContentTitle("DroidRemote Recepteur actif").setContentText("Cette tablette peut etre controlee").setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(1,notif)
    }
}
