package com.droidremote.universal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.InetSocketAddress
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.io.OutputStream

class TvRemoteClient(private val tvIp: String) {
    private var tlsSocket: java.net.Socket? = null

    suspend fun triggerPinAndConnect(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Connexion TLS sur 6466 pour forcer affichage PIN
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }), SecureRandom())
            val factory = ctx.socketFactory
            val s = factory.createSocket()
            s.connect(InetSocketAddress(tvIp, 6466), 3000)
            s.soTimeout = 5000
            tlsSocket = s
            
            // Envoie RemoteConfigure pour déclencher PIN (protobuf simplifié en JSON pour trigger)
            val out: OutputStream = s.getOutputStream()
            // Message qui force la TV à afficher le code
            val payload = "{\"type\":\"remoteConfigure\",\"model\":\"DroidRemote\",\"vendor\":\"Google\",\"name\":\"DroidRemote\"}\n"
            out.write(payload.toByteArray())
            out.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback 6467
            try {
                val ctx = SSLContext.getInstance("TLS")
                ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }), SecureRandom())
                val factory = ctx.socketFactory
                val s = factory.createSocket()
                s.connect(InetSocketAddress(tvIp, 6467), 3000)
                tlsSocket = s
                true
            } catch (e2: Exception) { false }
        }
    }

    suspend fun sendKey(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (tlsSocket == null || tlsSocket?.isClosed == true) {
                triggerPinAndConnect()
            }
            // Si on a un socket TLS, on envoie la touche via le protocole
            tlsSocket?.let { s ->
                val out = s.getOutputStream()
                val keyPayload = when(key) {
                    "UP" -> "KEYCODE_DPAD_UP"
                    "DOWN" -> "KEYCODE_DPAD_DOWN"
                    "LEFT" -> "KEYCODE_DPAD_LEFT"
                    "RIGHT" -> "KEYCODE_DPAD_RIGHT"
                    "OK" -> "KEYCODE_DPAD_CENTER"
                    "BACK" -> "KEYCODE_BACK"
                    "HOME" -> "KEYCODE_HOME"
                    "VOL_UP" -> "KEYCODE_VOLUME_UP"
                    "VOL_DOWN" -> "KEYCODE_VOLUME_DOWN"
                    else -> key
                }
                out.write("{\"type\":\"remoteKey\",\"key\":\"$keyPayload\"}\n".toByteArray())
                out.flush()
                return@withContext true
            }
            false
        } catch (e: Exception) { false }
    }

    suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val realApp = when(appId) { "YOUTUBE" -> "YouTube"; "NETFLIX" -> "Netflix"; else -> appId }
            val conn = URL("http://$tvIp:8008/apps/$realApp").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 2000
            val c = conn.responseCode
            c in 200..299 || c == 201 || c == 204
        } catch (e: Exception) { false }
    }
}
