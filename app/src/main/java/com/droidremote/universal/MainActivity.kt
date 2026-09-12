package com.droidremote.universal
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.net.Inet4Address

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = NetworkScanner(this)
        setContent {
            var isControllerMode by remember { mutableStateOf(true) }
            var selected by remember { mutableStateOf<AndroidDevice?>(null) }
            var receiverRunning by remember { mutableStateOf(false) }
            val devices by scanner.devices.collectAsState()
            val isScanning by scanner.isScanning.collectAsState()
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0E14)) {
                    if (selected != null) {
                        TabletControlScreen(device = selected!!, scanner = scanner, onBack = { selected = null })
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("DroidRemote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if(isControllerMode) "Controleur Tablette" else "Recepteur Tablette", color = Color(0xFF3DDC84))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2332), RoundedCornerShape(12.dp)).padding(4.dp)) {
                                FilterChip(selected = isControllerMode, onClick = { isControllerMode = true }, label = { Text("Controleur") }, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(selected = !isControllerMode, onClick = { isControllerMode = false }, label = { Text("Recepteur") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!isControllerMode) {
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TABLETTE A CONTROLER", color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) { Text("1. Ouvrir Accessibilite") }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { 
                                            val intent = Intent(this@MainActivity, TabletReceiverService::class.java)
                                            startForegroundService(intent)
                                            receiverRunning = true
                                        }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if(receiverRunning) Color.Gray else Color(0xFF3DDC84), contentColor = Color.Black)) { 
                                            Text(if(receiverRunning) "RECEIVER ACTIF" else "2. DEMARRER RECEPTEUR", fontWeight = FontWeight.Bold) 
                                        }
                                        if(receiverRunning) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(getLocalIp()+":8080", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            } else {
                                Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SCAN ${devices.size}...")
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SCANNER LES TABLETTES", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                if (devices.isEmpty() && !isScanning) {
                                    Text("Aucune tablette trouvee. Assure-toi que la tablette a lance le Recepteur et est sur le meme WiFi.", color = Color.Gray)
                                }
                                LazyColumn {
                                    items(devices.filter { it.type == DeviceType.TABLET || it.isReceiverInstalled }) { dev ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = dev }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp)) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(48.dp).background(Color(0xFF8B5CF6), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                    Text("TAB", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text("${dev.ip} • Tablette", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                                }
                                                if(dev.isReceiverInstalled) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3DDC84))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletControlScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    var textToSend by remember { mutableStateOf("") }
    var lastStatus by remember { mutableStateOf("Pret") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(device.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text("${device.ip} • Tablette", color = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(8.dp))
        Text(lastStatus, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Controles", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn("Lock") { scanner.sendCommand(device, "lock"); lastStatus = "Lock envoye" }
            ActionBtn("Home") { scanner.sendCommand(device, "home"); lastStatus = "Home envoye" }
            ActionBtn("Back") { scanner.sendCommand(device, "back"); lastStatus = "Back envoye" }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionBtn("Vol +") { scanner.sendCommand(device, "volup"); lastStatus = "Vol + envoye" }
            ActionBtn("Vol -") { scanner.sendCommand(device, "voldown"); lastStatus = "Vol - envoye" }
            ActionBtn("Recents") { scanner.sendCommand(device, "recents"); lastStatus = "Recents envoye" }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Clavier a distance", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = textToSend, onValueChange = { textToSend = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Texte a taper sur tablette") })
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { 
            if (textToSend.isNotEmpty()) {
                scanner.sendCommand(device, "input text $textToSend")
                lastStatus = "Texte envoye: $textToSend"
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) { 
            Text("Envoyer texte", fontWeight = FontWeight.Bold) 
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Lancer app", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scanner.sendCommand(device, "launch com.google.android.youtube"); lastStatus = "YouTube lance" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("YouTube") }
            Button(onClick = { scanner.sendCommand(device, "launch com.netflix.mediaclient"); lastStatus = "Netflix lance" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Netflix") }
            Button(onClick = { scanner.sendCommand(device, "launch com.android.chrome"); lastStatus = "Chrome lance" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332))) { Text("Chrome") }
        }
    }
}

@Composable
fun ActionBtn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(8.dp)) { Text(label, color = Color.White) }
}

fun getLocalIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val ip = addr.hostAddress ?: continue
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {}
    return "192.168.1.x"
}
