package com.droidremote.universal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class TvRemoteClient(private val tvIp: String) {
    private val bboxRouterIp = "192.168.1.254"

    suspend fun sendKey(key: String): Boolean = withContext(Dispatchers.IO) {
        if (tryBboxApi(key)) return@withContext true
        if (tryCastInternal(key)) return@withContext true
        tryHttpTv(key)
    }

    private fun tryBboxApi(key: String): Boolean {
        return try {
            val bboxKey = when(key) {
                "UP" -> "up"
                "DOWN" -> "down"
                "LEFT" -> "left"
                "RIGHT" -> "right"
                "OK" -> "ok"
                "BACK" -> "back"
                "HOME" -> "home"
                "VOL_UP" -> "vol_up"
                "VOL_DOWN" -> "vol_down"
                else -> key.lowercase()
            }
            val urls = listOf(
                "http://$bboxRouterIp/api/v1/tv/remote?key=$bboxKey",
                "http://$bboxRouterIp/api/v1/tv/1/remote?key=$bboxKey",
                "http://mabbox.bytel.fr/api/v1/tv/remote?key=$bboxKey",
                "http://$tvIp:8080/remote?key=$bboxKey"
            )
            for (u in urls) {
                try {
                    val conn = URL(u).openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 1500
                    if (conn.responseCode in 200..299) return true
                } catch (e: Exception) {}
            }
            false
        } catch (e: Exception) { false }
    }

    private fun tryCastInternal(key: String): Boolean {
        return try {
            when(key) {
                "YOUTUBE" -> { launchAppBlocking("YouTube"); true }
                "NETFLIX" -> { launchAppBlocking("Netflix"); true }
                else -> false
            }
        } catch (e: Exception) { false }
    }

    suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        launchAppBlocking(appId)
    }

    private fun launchAppBlocking(appId: String): Boolean {
        return try {
            val realApp = when(appId) {
                "YOUTUBE" -> "YouTube"
                "NETFLIX" -> "Netflix"
                else -> appId
            }
            val urls = listOf("http://$tvIp:8008/apps/$realApp", "http://$bboxRouterIp:8008/apps/$realApp")
            for (url in urls) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 2000
                    val c = conn.responseCode
                    if (c in 200..299 || c == 201 || c == 204) return true
                } catch (e: Exception) {}
            }
            false
        } catch (e: Exception) { false }
    }

    private fun tryHttpTv(key: String): Boolean {
        return try {
            val code = when(key) {
                "UP" -> "19"
                "DOWN" -> "20"
                "LEFT" -> "21"
                "RIGHT" -> "22"
                "OK" -> "23"
                "BACK" -> "4"
                "HOME" -> "3"
                "VOL_UP" -> "24"
                "VOL_DOWN" -> "25"
                else -> return false
            }
            val conn = URL("http://$tvIp:8080/key?code=$code").openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.responseCode in 200..299
        } catch (e: Exception) { false }
    }
}
