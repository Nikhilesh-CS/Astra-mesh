package com.astramesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astramesh.app.network.MeshProtocol
import com.astramesh.app.network.TorManager
import com.astramesh.app.network.TorState
import com.astramesh.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    navController: NavController,
    torManager: TorManager
) {
    val torState by torManager.torState.collectAsState()
    val onionAddress by torManager.onionAddress.collectAsState()
    val torLogs by torManager.torLogs.collectAsState()
    val lastError by torManager.lastError.collectAsState()
    val lastPing by torManager.lastPing.collectAsState()

    var testOnion by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var torVersionResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Watch for pong response
    LaunchedEffect(lastPing) {
        if (lastPing != null && testResult?.contains("Waiting") == true) {
            testResult = "Success ($lastPing)"
        }
    }

    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = { Text("Tor Diagnostics", color = SoftWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlack)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tor Status: ${if (torState is TorState.Connected) "Connected" else if (torState is TorState.Failed) "Failed" else "Starting"}", color = AccentCyan)
                        val progress = if (torState is TorState.Starting) (torState as TorState.Starting).progress else if (torState is TorState.Connected) 100 else 0
                        Text("Bootstrap: $progress%", color = SoftWhite)
                        Text("My Onion: ${if (onionAddress.isNotBlank()) onionAddress else "N/A"}", color = SoftWhite)
                        Text("SOCKS Port: 9050", color = SoftWhite)
                        Text("Hidden Service Port: 8765", color = SoftWhite)
                        Text("Last Error: ${lastError ?: "None"}", color = AccentPink)
                        Text("Last Ping: ${lastPing ?: "None"}", color = NeonGreen)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Binary Path: ${torManager.torBinaryPath}", color = SoftWhite, fontSize = 12.sp)
                        Text("File Exists: ${torManager.torBinaryExists}", color = SoftWhite, fontSize = 12.sp)
                        Text("Can Execute: ${torManager.torBinaryExecutable}", color = SoftWhite, fontSize = 12.sp)
                        Text("Binary Type: ${torManager.torBinaryType}", color = SoftWhite, fontSize = 12.sp)
                        Text("Detected ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"}", color = SoftWhite, fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    torVersionResult = torManager.testTorBinary()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Tor Binary")
                        }
                        if (torVersionResult != null) {
                            Text("Tor Test: $torVersionResult", color = AccentCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text("Test Tor Connection", color = AccentViolet, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = testOnion,
                    onValueChange = { testOnion = it },
                    label = { Text("Peer Onion Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        testResult = "Connecting..."
                        scope.launch(Dispatchers.IO) {
                            val payload = MeshProtocol.encodePing(System.currentTimeMillis(), onionAddress)
                            val ok = torManager.sendToOnion(testOnion, payload)
                            if (ok) {
                                testResult = "Ping sent... Waiting for pong..."
                            } else {
                                testResult = "Connection Timeout"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ping")
                }
                if (testResult != null) {
                    Text("Result: $testResult", color = MutedGray)
                }
            }

            item {
                Text("Tor Logs", color = AccentViolet, fontWeight = FontWeight.Bold)
            }

            items(torLogs.reversed()) { log ->
                Text(log, color = MutedGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
