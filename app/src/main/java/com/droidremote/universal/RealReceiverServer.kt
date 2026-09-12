package com.droidremote.universal
import fi.iki.elonen.NanoHTTPD
import android.content.Context
class RealReceiverServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri; val params = session.parms
        try {
            when {
                uri == "/ping" -> return newFixedLengthResponse("pong")
                uri == "/command" -> {
                    val cmd = params["cmd"] ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "no cmd")
                    when(cmd) {
                        "lock" -> Runtime.getRuntime().exec("input keyevent 26")
                        "unlock" -> Runtime.getRuntime().exec("input keyevent 82")
                        "volup" -> Runtime.getRuntime().exec("input keyevent 24")
                        "voldown" -> Runtime.getRuntime().exec("input keyevent 25")
                        "home" -> Runtime.getRuntime().exec("input keyevent 3")
                        "back" -> Runtime.getRuntime().exec("input keyevent 4")
                    }
                    return newFixedLengthResponse("ok:$cmd")
                }
                uri == "/tap" -> {
                    val x = params["x"]?.toFloatOrNull() ?: 0f; val y = params["y"]?.toFloatOrNull() ?: 0f
                    RemoteAccessibilityService.instance?.click(x, y)
                    return newFixedLengthResponse("tap $x $y")
                }
                uri == "/text" -> {
                    val text = params["text"] ?: ""
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "input text '$text'"))
                    return newFixedLengthResponse("text:$text")
                }
                uri == "/key" -> {
                    val code = params["code"] ?: "26"
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent $code"))
                    return newFixedLengthResponse("key $code")
                }
                uri == "/screenshot" -> {
                    Runtime.getRuntime().exec(arrayOf("sh", "-c", "screencap -p /sdcard/droid_screenshot.png"))
                    return newFixedLengthResponse("screenshot saved")
                }
            }
        } catch (e: Exception) { return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.message) }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
    }
}
