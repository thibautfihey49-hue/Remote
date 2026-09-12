package com.droidremote.universal
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        val scanner = NetworkScanner()
        setContent {
            var selected by remember { mutableStateOf<AndroidDevice?>(null) }
            var isControllerMode by remember { mutableStateOf(true) }
            val devices by scanner.devices.collectAsState()
            val isScanning by scanner.isScanning.collectAsState()
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A0E14)) {
                    if (selected == null) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("DroidRemote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Télécommande Universelle", color = Color(0xFF3DDC84))
                                }
                                Badge { Text("${devices.size} appareils") }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth().background(Color(0xFF1A2332), RoundedCornerShape(12.dp)).padding(4.dp))) {
                                FilterChip(selected = isControllerMode, onClick = { isControllerMode = true }, label = { Text("🎮 Contrôleur") }, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                FilterChip(selected = !isControllerMode, onClick = { isControllerMode = false }, label = { Text("📲 Récepteur") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                if (isScanning) CircularProgressIndicator(Modifier.size(20.dp))
                                else { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("SCANNER LE RÉSEAU", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.height(16.dp))
                            LazyColumn {
                                items(devices) { dev ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = dev }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(16.dp)) {
                                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(48.dp).background(if(dev.type==DeviceType.TABLET) Color(0xFF8B5CF6) else Color(0xFF3DDC84), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Text(if(dev.type==DeviceType.TV) "📺" else if(dev.type==DeviceType.TABLET) "📱" else "📲")
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text("${dev.type} • ${dev.ip} • ${dev.battery}%", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                            }
                                            if(dev.isReceiverInstalled) { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF3DDC84)) }
                                        }
                                    }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoteDetailScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text(device.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        if (device.type == DeviceType.TABLET || device.type == DeviceType.PHONE) {
            Box(Modifier.fillMaxWidth().height(280.dp).background(Color.Black, RoundedCornerShape(16.dp)).border(2.dp, Color(0xFF3DDC84), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🖥️ Miroir: ${device.name}", color = Color.White)
                    Text("Écran en direct", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("⬤ REC", color = Color.Red)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Contrôle Tablette", color = Color.White, fontWeight = FontWeight.Bold)
            FlowRow {
                Btn("🔒 Verrouiller") { scanner.sendCommand(device, "lock") }
                Btn("🔓 Déverrouiller") { scanner.sendCommand(device, "unlock") }
                Btn("🔊 Vol +") { scanner.sendCommand(device, "volup") }
                Btn("🔉 Vol -") { scanner.sendCommand(device, "voldown") }
                Btn("📸 Screenshot") { scanner.sendCommand(device, "screenshot") }
                Btn("📍 Sonner") { scanner.sendCommand(device, "ring") }
            }
            Spacer(Modifier.height(12.dp))
            var text by remember { mutableStateOf("") }
            Row {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Taper sur tablette...") })
                Spacer(Modifier.width(8.dp))
                Button(onClick = { scanner.sendCommand(device, "input text $text") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))) { Text("Envoyer") }
            }
        } else {
            Text("Télécommande TV", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Column(Modifier.align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { scanner.sendCommand(device, "up") }) { Text("▲") }
                Row {
                    Button(onClick = { scanner.sendCommand(device, "left") }) { Text("◀") }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = { scanner.sendCommand(device, "ok") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))) { Text("OK") }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = { scanner.sendCommand(device, "right") }) { Text("▶") }
                }
                Button(onClick = { scanner.sendCommand(device, "down") }) { Text("▼") }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { scanner.sendCommand(device, "home") }) { Text("HOME") }
                Button(onClick = { scanner.sendCommand(device, "back") }) { Text("RETOUR") }
                Button(onClick = { scanner.sendCommand(device, "power") }) { Text("POWER") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { scanner.sendCommand(device, "youtube") }) { Text("YouTube") }
                Button(onClick = { scanner.sendCommand(device, "netflix") }) { Text("Netflix") }
                Button(onClick = { scanner.sendCommand(device, "spotify") }) { Text("Spotify") }
            }
        }
    }
}
@Composable
fun Btn(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)), shape = RoundedCornerShape(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
