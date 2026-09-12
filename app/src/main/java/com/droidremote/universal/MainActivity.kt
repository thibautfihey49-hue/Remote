package com.droidremote.universal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
            var selected by remember { mutableStateOf<AndroidDevice?>(null) }
            val devices by scanner.devices.collectAsState()
            val isScanning by scanner.isScanning.collectAsState()
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0E14)) {
                    if (selected == null) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("DroidRemote REAL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Vrai scan reseau + controle", color = Color(0xFF3DDC84))
                                }
                                Badge { Text("${devices.size} trouves") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                if (isScanning) { CircularProgressIndicator(modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("SCAN ${devices.size}...") }
                                else { Icon(Icons.Default.Search, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("SCANNER VRAI RESEAU", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Scan 192.168.1.1-254 + mDNS Chromecast/TV + port 8080 receiver", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn {
                                items(devices) { dev ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = dev }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp)) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(48.dp).background(if(dev.type==DeviceType.TABLET) Color(0xFF8B5CF6) else Color(0xFF3DDC84), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Text(text = if(dev.type==DeviceType.TV) "TV" else if(dev.type==DeviceType.TABLET) "TAB" else "CAST", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text("${dev.type} • ${dev.ip}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if(dev.isReceiverInstalled) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3DDC84)) }
                                        }
                                    }
                                }
                                if(devices.isEmpty() && !isScanning) {
                                    item { Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Aucun appareil", color = Color.Gray)
                                        Text("Meme WiFi + tablette en Mode Recepteur", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }}
                                }
                            }
                        }
                    } else {
                        RemoteDetailScreen(device = selected!!, scanner = scanner, onBack = { selected = null })
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteDetailScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White) }
            Text(device.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(device.ip + " - " + device.type, color = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(12.dp))
        if (device.isReceiverInstalled) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color(0xFF1A2332), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECEIVER ACTIF", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold)
                    Text("http://${device.ip}:8080", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Controle REEL", color = Color.White, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Lock") { scanner.sendCommand(device, "lock") }
                ActionBtn("Unlock") { scanner.sendCommand(device, "unlock") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Vol +") { scanner.sendCommand(device, "volup") }
                ActionBtn("Vol -") { scanner.sendCommand(device, "voldown") }
                ActionBtn("Home") { scanner.sendCommand(device, "home") }
                ActionBtn("Back") { scanner.sendCommand(device, "back") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            var text by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Tape...") })
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { scanner.sendCommand(device, "input text $text") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))) { Text("Envoyer", color = Color.Black) }
            }
        } else {
            Text("Device detecte: ${device.ip}", color = Color.White)
        }
    }
}
@Composable
fun ActionBtn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(2.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
