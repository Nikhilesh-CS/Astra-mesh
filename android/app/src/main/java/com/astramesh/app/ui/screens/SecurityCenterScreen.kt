package com.astramesh.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.astramesh.app.ui.theme.AstraTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astramesh.app.crypto.CryptoManager
import com.astramesh.app.identity.IdentityManager
import com.astramesh.app.network.TorManager
import com.astramesh.app.ui.theme.AccentCyan
import com.astramesh.app.ui.theme.CardSurface
import com.astramesh.app.ui.theme.DimGray
import com.astramesh.app.ui.theme.MutedGray
import com.astramesh.app.ui.theme.NeonGreen
import com.astramesh.app.ui.theme.SoftWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    identityManager: IdentityManager,
    torManager: TorManager
) {
    val context = LocalContext.current
    val torReady by torManager.isTorReady.collectAsStateWithLifecycle()
    val torStatus by torManager.torStatus.collectAsStateWithLifecycle()
    val onionAddress by torManager.onionAddress.collectAsStateWithLifecycle()
    val identity = identityManager.loadIdentity()
    val identityKey = identity?.signingPublicKey?.let { CryptoManager.toHex(it) } ?: "Unknown"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Security Center", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(AstraTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(AstraTheme.spacing.standard)
        ) {
            SecurityCard(
                title = "End-To-End Encryption",
                value = "Active (X25519, ChaCha20-Poly1305)",
                statusColor = NeonGreen,
                icon = "🔒"
            )
            
            SecurityCard(
                title = "Identity Key",
                value = identityKey,
                statusColor = SoftWhite,
                icon = "🔑",
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Identity Key", identityKey))
                    Toast.makeText(context, "Identity Key copied", Toast.LENGTH_SHORT).show()
                }
            )

            SecurityCard(
                title = "Tor Network Status",
                value = if (torReady) "Connected" else "Connecting / Offline ($torStatus)",
                statusColor = if (torReady) NeonGreen else AstraTheme.colors.secondary,
                icon = "🧅"
            )

            SecurityCard(
                title = "Onion Address",
                value = onionAddress.ifBlank { "Not available yet" },
                statusColor = if (onionAddress.isNotBlank()) AccentCyan else MutedGray,
                icon = "🌐",
                onCopy = {
                    if (onionAddress.isNotBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Onion Address", onionAddress))
                        Toast.makeText(context, "Onion Address copied", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            SecurityCard(
                title = "Nearby Connectivity",
                value = "Bluetooth / Wi-Fi Direct Active",
                statusColor = NeonGreen,
                icon = "📶"
            )
        }
    }
}

@Composable
fun SecurityCard(
    title: String,
    value: String,
    statusColor: Color,
    icon: String,
    onCopy: (() -> Unit)? = null
) {
    var modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(AstraTheme.spacing.standard))
        .background(CardSurface)
    
    if (onCopy != null) {
        modifier = modifier.clickable { onCopy() }
    }

    Row(
        modifier = modifier.padding(AstraTheme.spacing.standard),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = AstraTheme.typography.headlineLarge.fontSize)
        Spacer(modifier = Modifier.width(AstraTheme.spacing.standard))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = AstraTheme.typography.bodySmall.fontSize,
                color = MutedGray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(AstraTheme.spacing.tiny))
            Text(
                text = value,
                fontSize = AstraTheme.typography.bodyLarge.fontSize,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

