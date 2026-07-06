package com.astramesh.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astramesh.app.data.AppDatabase
import com.astramesh.app.engine.MessagePayload
import com.astramesh.app.engine.TransportType
import com.astramesh.app.network.MessageRouter
import com.astramesh.app.network.NearbyConnectionManager
import com.astramesh.app.transfer.MediaTransferManager
import com.astramesh.app.ui.components.*
import com.astramesh.app.ui.screens.chat.ChatViewModel
import com.astramesh.app.ui.screens.chat.SmartScrollEngine
import com.astramesh.app.ui.theme.AstraTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactKey: String,
    navController: NavController,
    db: AppDatabase,
    nearbyManager: NearbyConnectionManager,
    messageRouter: MessageRouter,
    mediaTransferManager: MediaTransferManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Instantiate ViewModel at Compose level for simplicity. Normally we'd use ViewModelProvider.
    val viewModel = remember(contactKey) { ChatViewModel(contactKey, db, messageRouter) }
    
    val contactName by viewModel.contactName.collectAsState()
    val contactEndpoint by viewModel.contactEndpoint.collectAsState()
    val contactOnion by viewModel.contactOnion.collectAsState()
    
    val connectedEndpoints by nearbyManager.connectedEndpoints.collectAsState()
    val isNearbyOnline = connectedEndpoints.contains(contactEndpoint)
    val isOnline = isNearbyOnline || contactOnion.isNotBlank()
    
    val messages by viewModel.conversationEngine.messages.collectAsState()
    
    val listState = rememberLazyListState()
    val smartScrollEngine = remember(listState) { SmartScrollEngine(listState, coroutineScope) }
    
    var messageText by remember { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<MessagePayload?>(null) }
    var showMessageMenu by remember { mutableStateOf<MessagePayload?>(null) }
    
    // Scroll handling when new messages arrive
    LaunchedEffect(messages.size) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null) {
            smartScrollEngine.onNewMessageArrived(lastMessage.senderId == "me")
        }
    }

    Scaffold(
        containerColor = AstraTheme.colors.background,
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(color = AstraTheme.colors.surface, shadowElevation = AstraTheme.spacing.tiny) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AstraTheme.spacing.small, vertical = AstraTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = AstraTheme.colors.onSurface)
                    }
                    AstraAvatar(
                        name = contactName,
                        size = AstraTheme.spacing.massive2,
                        isOnline = isOnline
                    )
                    Spacer(modifier = Modifier.width(AstraTheme.spacing.medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contactName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AstraTheme.colors.onSurface)
                        
                        ConnectionStatusPill(
                            transportType = when {
                                isNearbyOnline -> com.astramesh.app.ui.components.TransportType.BLUETOOTH
                                contactOnion.isNotBlank() -> com.astramesh.app.ui.components.TransportType.TOR
                                else -> com.astramesh.app.ui.components.TransportType.OFFLINE
                            },
                            details = if (isNearbyOnline) contactEndpoint else if (contactOnion.isNotBlank()) "Connected" else ""
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = AstraTheme.colors.background, shadowElevation = AstraTheme.spacing.standard) {
                Column {
                    if (replyToMessage != null) {
                        ReplyPreviewBanner(
                            message = replyToMessage!!,
                            onCancel = { replyToMessage = null }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AstraTheme.spacing.medium, vertical = 10.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(
                            onClick = { /* TODO: Open AttachmentSheet */ },
                            modifier = Modifier.padding(bottom = AstraTheme.spacing.tiny)
                        ) {
                            Icon(Icons.Rounded.AttachFile, "Attach", tint = AstraTheme.colors.onSurfaceVariant)
                        }
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message...", color = AstraTheme.colors.onSurfaceVariant) },
                            shape = RoundedCornerShape(AstraTheme.spacing.extraLarge),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AstraTheme.colors.primary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = AstraTheme.colors.onSurface,
                                unfocusedTextColor = AstraTheme.colors.onSurface,
                                cursorColor = AstraTheme.colors.primary,
                                focusedContainerColor = AstraTheme.colors.surface,
                                unfocusedContainerColor = AstraTheme.colors.surface
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(AstraTheme.spacing.small))
                        
                        val isSendEnabled = messageText.isNotBlank()
                        val sendScale by animateFloatAsState(targetValue = if (isSendEnabled) 1f else 0.8f, label = "sendBtn")
                        
                        IconButton(
                            onClick = {
                                if (!isSendEnabled) return@IconButton
                                viewModel.sendMessage(messageText, replyToMessage?.id)
                                messageText = ""
                                replyToMessage = null
                            },
                            enabled = isSendEnabled,
                            modifier = Modifier
                                .padding(bottom = AstraTheme.spacing.tiny)
                                .size(AstraTheme.spacing.massive3)
                                .scale(sendScale)
                                .clip(CircleShape)
                                .background(
                                    if (isSendEnabled) AstraTheme.colors.primary else AstraTheme.colors.surfaceVariant
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = AstraTheme.colors.onPrimary, modifier = Modifier.size(AstraTheme.spacing.large))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (messages.isEmpty()) {
                AstraEmptyState(
                    title = "No messages yet",
                    message = "Say hello to $contactName!"
                )
            } else {
                val reversedMessages = remember(messages) { messages.asReversed() }
                val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = AstraTheme.spacing.standard, vertical = AstraTheme.spacing.medium)
                ) {
                    items(reversedMessages.size) { index ->
                        val message = reversedMessages[index]
                        val nextMessage = reversedMessages.getOrNull(index + 1)
                        
                        val dateStr = dateFormat.format(Date(message.timestamp))
                        val nextDateStr = nextMessage?.let { dateFormat.format(Date(it.timestamp)) }
                        
                        MessageBubbleProxy(
                            message = message,
                            onLongClick = { showMessageMenu = message },
                            onSwipeReply = { replyToMessage = message }
                        )
                        
                        if (dateStr != nextDateStr) {
                            DateSeparator(dateStr)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleProxy(message: MessagePayload, onLongClick: () -> Unit, onSwipeReply: () -> Unit) {
    val isSent = message.senderId == "me"
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val accentColor = if (isSent) AstraTheme.colors.onPrimary else AstraTheme.colors.primary

    com.astramesh.app.ui.adaptive.AdaptiveChatBubble(
        isMine = isSent,
        timestamp = timeFormat.format(Date(message.timestamp)),
        lifecycleState = message.lifecycleState,
        isEncrypted = message.isEncrypted,
        modifier = Modifier.padding(vertical = AstraTheme.spacing.tiny).combinedClickable(onClick = {}, onLongClick = onLongClick),
        replyContent = if (message.replyToId != null) {
            {
                com.astramesh.app.ui.adaptive.ReplyPreview(
                    senderName = "Replied",
                    messageText = "Original message text...", // TODO: map reply text from DB
                    accentColor = accentColor
                )
            }
        } else null
    ) {
        Text(
            text = message.text,
            color = if (isSent) AstraTheme.colors.onPrimary else AstraTheme.colors.onSurfaceVariant,
            style = AstraTheme.typography.bodyMedium
        )
    }
}

@Composable
fun DateSeparator(dateStr: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = AstraTheme.spacing.medium), contentAlignment = Alignment.Center) {
        Text(
            text = dateStr,
            fontSize = AstraTheme.typography.labelMedium.fontSize,
            color = AstraTheme.colors.onSurfaceVariant,
            modifier = Modifier.background(AstraTheme.colors.surface, RoundedCornerShape(AstraTheme.spacing.medium)).padding(horizontal = 10.dp, vertical = AstraTheme.spacing.tiny)
        )
    }
}

@Composable
fun ReplyPreviewBanner(message: MessagePayload, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraTheme.colors.surface)
            .padding(horizontal = AstraTheme.spacing.standard, vertical = AstraTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(AstraTheme.spacing.tiny).height(AstraTheme.spacing.massive1).background(AstraTheme.colors.primary, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(AstraTheme.spacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (message.senderId == "me") "You" else "Them", fontSize = AstraTheme.typography.bodySmall.fontSize, color = AstraTheme.colors.primary, fontWeight = FontWeight.Bold)
            Text(message.text, fontSize = AstraTheme.typography.bodySmall.fontSize, color = AstraTheme.colors.onSurfaceVariant, maxLines = 1)
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Rounded.Close, "Cancel", tint = AstraTheme.colors.onSurfaceVariant)
        }
    }
}
