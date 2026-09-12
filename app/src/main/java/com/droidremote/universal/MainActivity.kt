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
        val scanner = NetworkScanner()
        setContent {
            var selected by remember { mutableStateOf<AndroidDevice?>(null) }
            var isControllerMode by remember { mutableStateOf(true) }
            val devices by scanner.devices.collectAsState()
            val isScanning by scanner.isScanning.collectAsState()
            val scope = rememberCoroutineScope()

            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0E14)) {
                    if (selected == null) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "DroidRemote",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text("Telecommande Universelle", color = Color(0xFF3DDC84))
                                }
                                Badge { Text("${devices.size} appareils") }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color(0xFF1A2332), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                FilterChip(
                                    selected = isControllerMode,
                                    onClick = { isControllerMode = true },
                                    label = { Text("Controleur") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = !isControllerMode,
                                    onClick = { isControllerMode = false },
                                    label = { Text("Recepteur") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { scope.launch { scanner.scanNetwork() } },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3DDC84),
                                    contentColor = Color.Black
                                )
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SCANNER LE RESEAU", fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn {
                                items(devices) { dev ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable { selected = dev },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(48.dp)
                                                    .background(
                                                        if (dev.type == DeviceType.TABLET) Color(0xFF8B5CF6)
                                                        else Color(0xFF3DDC84),
                                                        RoundedCornerShape(12.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (dev.type == DeviceType.TV) "TV" else if (dev.type == DeviceType.TABLET) "TAB" else "PH",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(dev.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${dev.type} - ${dev.ip} - ${dev.battery}%",
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            if (dev.isReceiverInstalled) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF3DDC84)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        RemoteDetailScreen(
                            device = selected!!,
                            scanner = scanner,
                            onBack = { selected = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteDetailScreen(device: AndroidDevice, scanner: NetworkScanner, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF0A0E14))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                device.name,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (device.type == DeviceType.TABLET || device.type == DeviceType.PHONE) {
            Box(
                modifier = Modifier.fillMaxWidth().height(280.dp)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Miroir: ${device.name}", color = Color.White)
                    Text("Ecran en direct", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("REC", color = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Controle Tablette", color = Color.White, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Verrouiller") { scanner.sendCommand(device, "lock") }
                ActionBtn("Deverrouiller") { scanner.sendCommand(device, "unlock") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn("Vol +") { scanner.sendCommand(device, "volup") }
                ActionBtn("Vol -") { scanner.sendCommand(device, "voldown") }
                ActionBtn("Screenshot") { scanner.sendCommand(device, "screenshot") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            var text by remember { mutableStateOf("") }
            Text("Clavier a distance", color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Taper...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { scanner.sendCommand(device, "input text $text") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))
                ) {
                    Text("Envoyer", color = Color.Black)
                }
            }
        } else {
            Text("Telecommande TV", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { scanner.sendCommand(device, "up") }) { Text("HAUT") }
                Row {
                    Button(onClick = { scanner.sendCommand(device, "left") }) { Text("GAUCHE") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { scanner.sendCommand(device, "ok") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84))
                    ) {
                        Text("OK", color = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { scanner.sendCommand(device, "right") }) { Text("DROITE") }
                }
                Button(onClick = { scanner.sendCommand(device, "down") }) { Text("BAS") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { scanner.sendCommand(device, "home") }) { Text("HOME") }
                Button(onClick = { scanner.sendCommand(device, "back") }) { Text("RETOUR") }
                Button(onClick = { scanner.sendCommand(device, "power") }) { Text("POWER") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { scanner.sendCommand(device, "youtube") }) { Text("YouTube") }
                Button(onClick = { scanner.sendCommand(device, "netflix") }) { Text("Netflix") }
                Button(onClick = { scanner.sendCommand(device, "spotify") }) { Text("Spotify") }
            }
        }
    }
}

@Composable
fun ActionBtn(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2332)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

