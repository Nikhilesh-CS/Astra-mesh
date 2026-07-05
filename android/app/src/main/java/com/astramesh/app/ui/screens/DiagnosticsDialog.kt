package com.astramesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astramesh.app.network.NearbyConnectionManager
import com.astramesh.app.network.TorManager
import com.astramesh.app.network.TorState
import com.astramesh.app.ui.theme.*

@Composable
fun DiagnosticsDialog(
    torManager: TorManager,
    nearbyManager: NearbyConnectionManager,
    onDismiss: () -> Unit
) {
    val torState by torManager.torState.collectAsState()
    val torStatus by torManager.torStatus.collectAsState()
    val onionAddress by torManager.onionAddress.collectAsState()
    val isTorReady by torManager.isTorReady.collectAsState()
    
    val connectedEndpoints by nearbyManager.connectedEndpoints.collectAsState()
    val connectionStatus by nearbyManager.connectionStatus.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepBlack,
        title = {
            Text("Network Diagnostics", color = SoftWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tor Section
                Text("Tor Network", color = AccentViolet, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (torState) {
                                    is TorState.Connected -> NeonGreen
                                    is TorState.Starting -> AccentCyan
                                    is TorState.Failed -> AccentPink
                                    else -> DimGray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Status: $torStatus", color = MutedGray, fontSize = 13.sp)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text("Address: ${if (onionAddress.isNotBlank()) onionAddress else "Not available"}", color = MutedGray, fontSize = 13.sp)
                
                if (torState is TorState.Starting) {
                    val progress = (torState as TorState.Starting).progress
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bootstrap: $progress%", color = AccentCyan, fontSize = 12.sp)
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = AccentCyan,
                        trackColor = DarkSurface
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mesh Section
                Text("Mesh Network", color = AccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connected Peers: ${connectedEndpoints.size}", color = MutedGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Discovery: $connectionStatus", color = MutedGray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(24.dp))

                // Device Info
                Text("Device Info", color = DimGray, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("App Version: 2.0-mesh", color = MutedGray, fontSize = 13.sp)
                Text("Protocol: V2", color = MutedGray, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentViolet)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
