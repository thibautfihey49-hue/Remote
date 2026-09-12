package com.droidremote.universal
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
class RemoteAccessibilityService : AccessibilityService() {
    companion object { var instance: RemoteAccessibilityService? = null }
    override fun onServiceConnected() { instance = this; super.onServiceConnected() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    fun click(x: Float, y: Float) {
        val path = Path(); path.moveTo(x, y)
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 100)).build()
        dispatchGesture(gesture, null, null)
    }
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path(); path.moveTo(x1, y1); path.lineTo(x2, y2)
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 300)).build()
        dispatchGesture(gesture, null, null)
    }
}
