package com.droidremote.universal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class TvRemoteClient(private val tvIp: String) {
    // BBox routeur = 192.168.1.254, TV = 192.168.1.31
    private val bboxRouterIp = "192.168.1.254"

    suspend fun sendKey(key: String): Boolean = withContext(Dispatchers.IO) {
        // Methode 1: API BBox Bouygues (SANS PIN) - la plus fiable pour BBox
        if (tryBboxApi(key)) return@withContext true
        // Methode 2: Cast DIAL
        if (tryCast(key)) return@withContext true
        // Methode 3: HTTP direct TV
        tryHttpTv(key)
    }

    private fun tryBboxApi(key: String): Boolean {
        return try {
            // API BBox : http://192.168.1.254/api/v1/tv/remote
            // Keys: up, down, left, right, ok, back, home, vol_up, vol_down
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
            // Essaie plusieurs endpoints BBox
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

    private fun tryCast(key: String): Boolean {
        return try {
            // Pour lancer des apps via Cast, pas besoin de PIN
            when(key) {
                "YOUTUBE", "NETFLIX", "HOME" -> launchApp(key)
                else -> false
            }
        } catch (e: Exception) { false }
    }

    suspend fun launchApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val realApp = when(appId) {
                "YOUTUBE" -> "YouTube"
                "NETFLIX" -> "Netflix"
                "HOME" -> return@withContext sendKey("HOME")
                else -> appId
            }
            val urls = listOf(
                "http://$tvIp:8008/apps/$realApp",
                "http://$bboxRouterIp:8008/apps/$realApp"
            )
            for (url in urls) {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 2000
                    if (conn.responseCode in 200..299 || conn.responseCode == 201) return@withContext true
                } catch (e: Exception) {}
            }
            false
        } catch (e: Exception) { false }
    }

    private fun tryHttpTv(key: String): Boolean {
        return try {
            // Derniere tentative: keyevent via http si TV a un serveur http
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
