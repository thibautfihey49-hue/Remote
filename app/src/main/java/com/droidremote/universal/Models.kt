package com.droidremote.universal
enum class DeviceType { TV, PHONE, TABLET, BOX, CHROMECAST, NEST }
data class AndroidDevice(val id: String, val name: String, val ip: String, val type: DeviceType, val androidVersion: String, val battery: Int, val isOnline: Boolean = true, val isReceiverInstalled: Boolean = false)
