package com.droidremote.universal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.net.InetSocketAddress
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate

class TvRemoteClient(private val ip: String) {
    private var secureSocket: Socket? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }), SecureRandom())
            val factory = ctx.socketFactory
            val s = factory.createSocket()
            s.connect(InetSocketAddress(ip, 6466), 3000)
            secureSocket = s
            s.isConnected
        } catch (e: Exception) {
            try {
                val ctx = SSLContext.getInstance("TLS")
                ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }), SecureRandom())
                val factory = ctx.socketFactory
                val s = factory.createSocket()
                s.connect(InetSocketAddress(ip, 6467), 3000)
                secureSocket = s
                s.isConnected
            } catch (e2: Exception) { false }
        }
    }

    suspend fun sendKey(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (secureSocket == null) connect()
            // Ici on enverrait le protobuf RemoteKey
            // Pour la v5 on envoie via le protocole Cast / DIAL pour lancer + log le key
            println("TV $ip SEND KEY $key")
            // Fallback DIAL pour Home/Apps
            when(key) {
                "HOME" -> launchApp("com.google.android.tvlauncher")
                "YOUTUBE" -> launchApp("YouTube")
                "NETFLIX" -> launchApp("Netflix")
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:8008/apps/$appId"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 2000
            val code = conn.responseCode
            code in 200..299 || code == 201 || code == 204
        } catch (e: Exception) { false }
    }
}
