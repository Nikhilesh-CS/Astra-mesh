package com.astramesh.app.ui.screens

import com.astramesh.app.ui.theme.AstraTheme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astramesh.app.ui.theme.*

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
                    "Paste a contact key (astra:…). Include Tor .onion for distant messaging.",
                    fontSize = AstraTheme.typography.bodySmall.fontSize,
                    color = MutedGray
                )
                Spacer(modifier = Modifier.height(AstraTheme.spacing.medium))
                OutlinedTextField(
                    value = contactString,
                    onValueChange = { contactString = it },
                    label = { Text("Contact Key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite
                    )
                )
            }
        },
        containerColor = DarkSurface,
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
