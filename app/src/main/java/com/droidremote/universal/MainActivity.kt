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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                        RemoteDetailScreen(device = selected!!, scanner = scanner, onBack = { selected = null })
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("DroidRemote REAL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(if(isControllerMode) "Mode Controleur - SANS ADB" else "Mode Recepteur Tablette - SANS ADB", color = Color(0xFF3DDC84))
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
                                        Text("1. Active Accessibilite DroidRemote", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) { Text("Ouvrir Accessibilite") }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("2. Lance le serveur", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val intent = Intent(this@MainActivity, TabletReceiverService::class.java)
                                                startForegroundService(intent)
                                                receiverRunning = true
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = if(receiverRunning) Color.Gray else Color(0xFF3DDC84), contentColor = Color.Black)
                                        ) { Text(if(receiverRunning) "RECEIVER ACTIF" else "DEMARRER SERVICE RECEPTEUR", fontWeight = FontWeight.Bold) }
                                        if(receiverRunning) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Serveur actif sur:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                                    Text("${getIp()} : 8080", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                    if (isScanning) { CircularProgressIndicator(modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("SCAN ${devices.size}...") }
                                    else { Icon(Icons.Default.Search, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("SCANNER VRAI RESEAU", fontWeight = FontWeight.Bold) }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Cherche 192.168.1.1-254 port 8080=tablette 6466=TV 8009=Cast", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(16.dp))
                                LazyColumn {
                                    items(devices) { dev ->
                                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = dev }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp)) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(48.dp).background(if(dev.type==DeviceType.TABLET) Color(0xFF8B5CF6) else Color(0xFF3DDC84), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                    Text(if(dev.type==DeviceType.TABLET) "TAB" else "TV", color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text("${dev.type} • ${dev.ip}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
    private fun getIp(): String {
        return try {
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            val ip = wm.connectionInfo.ipAddress
            String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        } catch (e: Exception) { "192.168.x.x" }
    }
}

@Composable
fun RemoteDetailScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(device.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text("${device.ip} - SANS ADB", color = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(12.dp))
        if (device.isReceiverInstalled) {
            Text("Controle REEL tablette", color = Color.White, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Lock") { scanner.sendCommand(device, "lock") }
                ActionBtn("Unlock") { scanner.sendCommand(device, "unlock") }
                ActionBtn("Home") { scanner.sendCommand(device, "home") }
                ActionBtn("Back") { scanner.sendCommand(device, "back") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Vol +") { scanner.sendCommand(device, "volup") }
                ActionBtn("Vol -") { scanner.sendCommand(device, "voldown") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            var text by remember { mutableStateOf("") }
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Texte a taper sur tablette") })
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { scanner.sendCommand(device, "input text $text") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))) { Text("Envoyer texte REEL", color = Color.Black) }
        } else {
            Text("Device TV/Cast detecte: ${device.ip}", color = Color.White)
            Text("Controle TV sans ADB via port 6466 en cours...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun ActionBtn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(8.dp)) { Text(label, color = Color.White) }
}
