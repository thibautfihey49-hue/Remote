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
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A0E14)) {
                    if (selected == null) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            // ... (fichier complet dans le .sh)
                        }
                    } else {
                        RemoteDetailScreen(device = selected!!, scanner = scanner, onBack = { selected = null })
                    }
                }
            }
        }
    }
}
