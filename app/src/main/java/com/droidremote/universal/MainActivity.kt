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
                            Text(if(isControllerMode) "Controleur - TV sans ADB + Tablette" else "Recepteur Tablette", color = Color(0xFF3DDC84))
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
                                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))) { Text("Ouvrir Accessibilite") }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(onClick = { val intent = Intent(this@MainActivity, TabletReceiverService::class.java); startForegroundService(intent); receiverRunning = true }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if(receiverRunning) Color.Gray else Color(0xFF3DDC84), contentColor = Color.Black)) { Text(if(receiverRunning) "RECEIVER ACTIF" else "DEMARRER RECEPTEUR", fontWeight = FontWeight.Bold) }
                                        if(receiverRunning) { Spacer(modifier = Modifier.height(12.dp)); Text("${getIp()}:8080", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold) }
                                    }
                                }
                            } else {
                                Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                    if (isScanning) { CircularProgressIndicator(modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("SCAN ${devices.size}...") }
                                    else { Icon(Icons.Default.Search, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("SCANNER VRAI RESEAU", fontWeight = FontWeight.Bold) }
                                }
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
                                                    Text("${dev.type} • ${dev.ip} • SANS ADB", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
    val scope = rememberCoroutineScope()
    val tvClient = remember { TvRemoteClient(device.ip) }
    var status by remember { mutableStateOf("Pret - API BBox sans PIN") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(device.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text("${device.ip} - ${device.type} SANS ADB", color = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(12.dp))

        if (device.isReceiverInstalled) {
            Text("Controle REEL tablette", color = Color.White, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Lock") { scanner.sendCommand(device, "lock") }
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
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CONTROLE TV BBOX SANS PIN", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold)
                    Text(status, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Utilise API BBox 192.168.1.254 + Cast - pas de code TV necessaire", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Telecommande", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { status = if(tvClient.sendKey("UP")) "UP OK" else "UP echec"; } }, modifier = Modifier.size(80.dp, 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332))) { Text("HAUT") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { tvClient.sendKey("LEFT") } }, modifier = Modifier.size(80.dp, 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332))) { Text("GAUCHE") }
                    Button(onClick = { scope.launch { tvClient.sendKey("OK") } }, modifier = Modifier.size(80.dp, 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) { Text("OK") }
                    Button(onClick = { scope.launch { tvClient.sendKey("RIGHT") } }, modifier = Modifier.size(80.dp, 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332))) { Text("DROITE") }
                }
                Button(onClick = { scope.launch { tvClient.sendKey("DOWN") } }, modifier = Modifier.size(80.dp, 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332))) { Text("BAS") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Back") { scope.launch { tvClient.sendKey("BACK") } }
                ActionBtn("Home") { scope.launch { tvClient.sendKey("HOME") } }
                ActionBtn("Vol +") { scope.launch { tvClient.sendKey("VOL_UP") } }
                ActionBtn("Vol -") { scope.launch { tvClient.sendKey("VOL_DOWN") } }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { tvClient.launchApp("YouTube") } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("YouTube") }
                Button(onClick = { scope.launch { tvClient.launchApp("Netflix") } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Netflix") }
            }
        }
    }
}
@Composable
fun ActionBtn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(8.dp)) { Text(label, color = Color.White) }
}
