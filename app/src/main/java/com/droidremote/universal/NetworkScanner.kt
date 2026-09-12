package com.droidremote.universal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
class NetworkScanner {
    private val _devices = MutableStateFlow<List<AndroidDevice>>(emptyList())
    val devices: StateFlow<List<AndroidDevice>> = _devices
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    suspend fun scanNetwork() {
        _isScanning.value = true
        delay(1500)
        _devices.value = listOf(
            AndroidDevice("1","TV Salon TCL","192.168.1.25",DeviceType.TV,"Android TV 12",100,true,false),
            AndroidDevice("2","Galaxy Tab S9","192.168.1.32",DeviceType.TABLET,"Android 14",78,true,true),
            AndroidDevice("3","Mi Box S","192.168.1.41",DeviceType.BOX,"Android TV 11",100),
            AndroidDevice("4","Chromecast 4K","192.168.1.18",DeviceType.CHROMECAST,"Google TV",100),
            AndroidDevice("5","Pixel Tablet","192.168.1.55",DeviceType.TABLET,"Android 14",45,true,true),
            AndroidDevice("6","Galaxy S24","192.168.1.67",DeviceType.PHONE,"Android 14",92,true,true),
            AndroidDevice("7","Lenovo Tab M11","192.168.1.72",DeviceType.TABLET,"Android 13",60,true,false),
        )
        _isScanning.value = false
    }
    fun sendCommand(device: AndroidDevice, command: String) {
        println("Send to ${device.name} [${device.ip}]: $command")
    }
}
