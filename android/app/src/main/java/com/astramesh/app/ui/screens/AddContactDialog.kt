package com.astramesh.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.astramesh.app.ui.theme.AccentViolet
import com.astramesh.app.ui.theme.AstraTheme
import com.astramesh.app.ui.theme.MutedGray
import com.astramesh.app.ui.theme.SoftWhite

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onContactAdded: (String) -> Unit
) {
    var contactString by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact", color = SoftWhite) },
        text = {
            Column {
                Text(
                    "Paste a contact key (astra:...). Include Tor .onion for distant messaging.",
                    fontSize = AstraTheme.typography.bodySmall.fontSize,
                    color = MutedGray
                )
                Spacer(modifier = Modifier.height(AstraTheme.spacing.medium))
                OutlinedTextField(
                    value = contactString,
                    onValueChange = { contactString = it },
                    label = { Text("Contact Key") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedContainerColor = Color.White.copy(alpha = 0.10f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.14f)
                    )
                )
            }
        },
        containerColor = Color(0xE6111827),
        shape = RoundedCornerShape(30.dp),
        titleContentColor = Color(0xFFF6F7FF),
        textContentColor = Color(0xFFB9C3D4),
        tonalElevation = 0.dp,
        confirmButton = {
            TextButton(
                onClick = { onContactAdded(contactString) },
                enabled = contactString.isNotBlank()
            ) {
                Text("Add", color = AccentViolet)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedGray)
            }
        }
    )
}
