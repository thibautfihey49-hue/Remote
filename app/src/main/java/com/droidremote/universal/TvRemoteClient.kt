package com.droidremote.universal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.io.InputStream
import java.io.OutputStream

class TvRemoteClient(private val tvIp: String) {
    private var socket: java.net.Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null

    // Encode protobuf simple
    private fun encodeVarInt(value: Int): ByteArray {
        val out = mutableListOf<Byte>()
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) { out.add(v.toByte()); break }
            else { out.add(((v and 0x7F) or 0x80).toByte()); v = v ushr 7 }
        }
        return out.toByteArray()
    }
    private fun encodeField(tag: Int, wire: Int, data: ByteArray): ByteArray {
        val key = (tag shl 3) or wire
        return encodeVarInt(key) + data
    }
    private fun encodeString(s: String): ByteArray {
        val b = s.toByteArray()
        return encodeVarInt(b.size) + b
    }

    suspend fun connectAndTriggerPin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }), SecureRandom())
            val factory = ctx.socketFactory
            val s = factory.createSocket()
            s.connect(InetSocketAddress(tvIp, 6466), 4000)
            s.soTimeout = 10000
            socket = s
            output = s.getOutputStream()
            input = s.getInputStream()

            // RemoteConfigure qui declenche le PIN (comme Google TV app)
            // remote_configure { code1=622, model="DroidRemote", vendor="Google", package="com.droidremote.universal" }
            // Construction manuelle protobuf RemoteMessage -> remoteConfigure
            val modelField = encodeField(1, 2, encodeString("DroidRemote"))
            val vendorField = encodeField(2, 2, encodeString("Google"))
            val packageField = encodeField(6, 2, encodeString("com.droidremote.universal"))
            val versionField = encodeField(7, 2, encodeString("1.0"))
            val codeField = encodeField(3, 0, encodeVarInt(622))
            val unknown1 = encodeField(4, 0, encodeVarInt(1))
            val unknown2 = encodeField(5, 0, encodeVarInt(1))
            
            val remoteConfigurePayload = codeField + modelField + vendorField + unknown1 + unknown2 + packageField + versionField
            val remoteConfigureWrapper = encodeField(1, 2, remoteConfigurePayload) // field 1 = remoteConfigure in RemoteMessage
            
            // RemoteMessage length-delimited
            val finalMsg = encodeVarInt(remoteConfigureWrapper.size) + remoteConfigureWrapper
            
            output?.write(finalMsg)
            output?.flush()
            
            // La TV doit maintenant afficher le PIN
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (socket == null) return@withContext false
            // remote_set_active { code = pin }
            val pinField = encodeField(1, 2, encodeString(pin))
            val secretField = encodeField(2, 0, encodeVarInt(0))
            val payload = pinField + secretField
            val wrapper = encodeField(2, 2, payload) // field 2 = remoteSetActive
            val finalMsg = encodeVarInt(wrapper.size) + wrapper
            output?.write(finalMsg)
            output?.flush()
            true
        } catch (e: Exception) { false }
    }

    suspend fun sendKey(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (socket == null || socket?.isClosed == true) {
                if (!connectAndTriggerPin()) return@withContext false
            }
            val keyCode = when(key) {
                "UP" -> 19
                "DOWN" -> 20
                "LEFT" -> 21
                "RIGHT" -> 22
                "OK" -> 23
                "BACK" -> 4
                "HOME" -> 3
                "VOL_UP" -> 24
                "VOL_DOWN" -> 25
                else -> 23
            }
            val keyField = encodeField(1, 0, encodeVarInt(keyCode))
            val directionField = encodeField(2, 0, encodeVarInt(0)) // PRESS
            val payload = keyField + directionField
            val wrapper = encodeField(3, 2, payload) // field 3 = remoteKey
            val finalMsg = encodeVarInt(wrapper.size) + wrapper
            output?.write(finalMsg)
            output?.flush()
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val real = if (appId == "YOUTUBE") "YouTube" else "Netflix"
            val conn = java.net.URL("http://$tvIp:8008/apps/$real").openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 2000
            conn.responseCode in 200..299 || conn.responseCode == 201
        } catch (e: Exception) { false }
    }
}
