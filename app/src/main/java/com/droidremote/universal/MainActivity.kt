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
                            Text("Controleur = PIN comme Play Store", color = Color(0xFF3DDC84))
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { scope.launch { scanner.scanNetwork() } }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                                if (isScanning) Text("SCAN ${devices.size}...")
                                else { Icon(Icons.Default.Search, null); Spacer(modifier = Modifier.width(8.dp)); Text("SCANNER RESEAU", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn {
                                items(devices) { dev ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = dev }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332))) {
                                        Row(modifier = Modifier.padding(16.dp)) {
                                            Column(modifier = Modifier.weight(1f)) { Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1); Text("${dev.ip} • ${dev.type}", color = Color.Gray) }
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
fun RemoteDetailScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val tvClient = remember { TvRemoteClient(device.ip) }
    var status by remember { mutableStateOf("Pret") }
    var pin by remember { mutableStateOf("") }
    var pinRequested by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0E14)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(device.ip, color = Color(0xFF3DDC84))
        Spacer(modifier = Modifier.height(12.dp))

        if (!device.isReceiverInstalled) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TV BBOX - PIN comme Play Store", color = Color(0xFF3DDC84), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(status, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        scope.launch {
                            status = "Connexion a ${device.ip}:6466..."
                            val ok = tvClient.connectAndTriggerPin()
                            if (ok) {
                                status = "Regarde ta TV ! Le code PIN 6 chiffres doit s'afficher maintenant"
                                pinRequested = true
                            } else {
                                status = "Echec - verifie meme WiFi"
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84), contentColor = Color.Black)) {
                        Text("1. AFFICHER PIN SUR TV", fontWeight = FontWeight.Bold)
                    }
                    if (pinRequested) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = pin, onValueChange = { pin = it }, placeholder = { Text("Entre PIN affiche sur TV") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                val ok = tvClient.sendPin(pin)
                                status = if (ok) "PIN envoye ! Teste les touches" else "PIN echec"
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("2. VALIDER PIN") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Telecommande", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { scope.launch { tvClient.sendKey("UP") } }) { Text("HAUT") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { tvClient.sendKey("LEFT") } }) { Text("GAUCHE") }
                    Button(onClick = { scope.launch { tvClient.sendKey("OK") } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))) { Text("OK") }
                    Button(onClick = { scope.launch { tvClient.sendKey("RIGHT") } }) { Text("DROITE") }
                }
                Button(onClick = { scope.launch { tvClient.sendKey("DOWN") } }) { Text("BAS") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { tvClient.sendKey("BACK") } }) { Text("Back") }
                Button(onClick = { scope.launch { tvClient.sendKey("HOME") } }) { Text("Home") }
                Button(onClick = { scope.launch { tvClient.sendKey("VOL_UP") } }) { Text("Vol +") }
                Button(onClick = { scope.launch { tvClient.sendKey("VOL_DOWN") } }) { Text("Vol -") }
            }
        } else {
            Text("Tablette avec Receiver", color = Color.White)
        }
    }
}
