package com.astramesh.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astramesh.app.crypto.CryptoManager
import com.astramesh.app.identity.IdentityManager
import com.astramesh.app.ui.theme.*

@Composable
fun SetupScreen(
    identityManager: IdentityManager,
    onIdentityCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DeepBlack,
                        Color(0xFF1a103d),
                        Color(0xFF0f172a),
                        DeepBlack
                    ),
                    start = Offset(offset, 0f),
                    end = Offset(offset + 500f, 1500f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo area
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentViolet, AccentBlue, AccentCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Astra Mesh",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = SoftWhite,
                letterSpacing = (-1).sp
            )

            Text(
                text = "Encrypted P2P + Tor Messaging",
                fontSize = 14.sp,
                color = MutedGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name", color = MutedGray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentViolet,
                    unfocusedBorderColor = DimGray,
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    cursorColor = AccentViolet,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Passphrase field
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase", color = MutedGray) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentViolet,
                    unfocusedBorderColor = DimGray,
                    focusedTextColor = SoftWhite,
                    unfocusedTextColor = SoftWhite,
                    cursorColor = AccentViolet,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Create button with gradient
            Button(
                onClick = {
                    if (name.isNotBlank() && passphrase.isNotBlank()) {
                        val identity = CryptoManager.generateIdentity(name)
                        identityManager.saveIdentity(identity)
                        onIdentityCreated()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = name.isNotBlank() && passphrase.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentViolet,
                    disabledContainerColor = CardSurface
                )
            ) {
                Text(
                    "Create Identity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nearby: Wi-Fi + Bluetooth mesh.\nDistant: encrypted via Tor (Orbot).\nNo central servers.",
                fontSize = 12.sp,
                color = DimGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
