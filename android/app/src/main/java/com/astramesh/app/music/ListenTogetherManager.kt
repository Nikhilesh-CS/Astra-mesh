package com.astramesh.app.music

import com.astramesh.app.network.MeshProtocol
import com.astramesh.app.network.MessageRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class ListenTogetherState(
    val sessionId: String = "",
    val noteId: String = "",
    val peerKey: String = "",
    val active: Boolean = false,
    val incomingInvite: Boolean = false,
    val awaitingResponse: Boolean = false,
    val lastEvent: ListenTogetherEvent? = null
)

class ListenTogetherManager(
    private val scope: CoroutineScope,
    private val messageRouter: MessageRouter
) {
    private val _state = MutableStateFlow(ListenTogetherState())
    val state: StateFlow<ListenTogetherState> = _state.asStateFlow()
    val playbackSyncController = PlaybackSyncController()

    fun inviteSession(peerKey: String, noteId: String, positionMs: Long) {
        val sessionId = UUID.randomUUID().toString()
        val event = ListenTogetherEvent(sessionId, noteId, peerKey, ListenTogetherEventType.INVITE, positionMs)
        _state.value = ListenTogetherState(
            sessionId = sessionId,
            noteId = noteId,
            peerKey = peerKey,
            active = false,
            incomingInvite = false,
            awaitingResponse = true,
            lastEvent = event
        )
        scope.launch(Dispatchers.IO) {
            messageRouter.sendRawPayload(peerKey, encodeEvent(event), MeshProtocol.TYPE_MUSIC_SYNC)
        }
    }

    fun startSession(peerKey: String, noteId: String, positionMs: Long) {
        val sessionId = UUID.randomUUID().toString()
        _state.value = ListenTogetherState(sessionId, noteId, peerKey, active = true)
        sendEvent(peerKey, noteId, sessionId, ListenTogetherEventType.PLAY, positionMs)
    }

    fun acceptIncomingInvite() {
        val current = _state.value
        if (!current.incomingInvite || current.peerKey.isBlank()) return
        sendEvent(current.peerKey, current.noteId, current.sessionId, ListenTogetherEventType.ACCEPT, current.lastEvent?.positionMs ?: 0L)
    }

    fun rejectIncomingInvite() {
        val current = _state.value
        if (!current.incomingInvite || current.peerKey.isBlank()) return
        sendEvent(current.peerKey, current.noteId, current.sessionId, ListenTogetherEventType.REJECT, 0L)
        _state.value = ListenTogetherState()
    }

    fun endSession() {
        val current = _state.value
        if (current.peerKey.isBlank()) return
        sendEvent(current.peerKey, current.noteId, current.sessionId, ListenTogetherEventType.END_SESSION, current.lastEvent?.positionMs ?: 0L)
        _state.value = ListenTogetherState()
    }

    fun sendEvent(
        peerKey: String,
        noteId: String,
        sessionId: String = _state.value.sessionId.ifBlank { UUID.randomUUID().toString() },
        type: ListenTogetherEventType,
        positionMs: Long
    ) {
        scope.launch(Dispatchers.IO) {
            val event = ListenTogetherEvent(sessionId, noteId, peerKey, type, positionMs)
            _state.value = when (type) {
                ListenTogetherEventType.ACCEPT -> ListenTogetherState(sessionId, noteId, peerKey, active = true, lastEvent = event)
                ListenTogetherEventType.REJECT, ListenTogetherEventType.END_SESSION -> ListenTogetherState()
                else -> ListenTogetherState(sessionId, noteId, peerKey, type in setOf(ListenTogetherEventType.PLAY, ListenTogetherEventType.PAUSE, ListenTogetherEventType.SEEK, ListenTogetherEventType.POSITION_SYNC), lastEvent = event)
            }
            messageRouter.sendRawPayload(peerKey, encodeEvent(event), MeshProtocol.TYPE_MUSIC_SYNC)
        }
    }

    fun handleSyncPacket(raw: String, senderKey: String) {
        runCatching {
            val json = JSONObject(raw)
            if (json.optString("astraType") != "music_sync") return@runCatching
            val event = ListenTogetherEvent(
                sessionId = json.getString("sessionId"),
                noteId = json.getString("noteId"),
                peerKey = senderKey,
                eventType = ListenTogetherEventType.valueOf(json.getString("eventType")),
                positionMs = json.optLong("positionMs", 0L),
                sentAt = json.optLong("sentAt", System.currentTimeMillis())
            )
            _state.value = when (event.eventType) {
                ListenTogetherEventType.INVITE -> ListenTogetherState(
                    sessionId = event.sessionId,
                    noteId = event.noteId,
                    peerKey = senderKey,
                    active = false,
                    incomingInvite = true,
                    awaitingResponse = false,
                    lastEvent = event
                )
                ListenTogetherEventType.ACCEPT -> ListenTogetherState(
                    sessionId = event.sessionId,
                    noteId = event.noteId,
                    peerKey = senderKey,
                    active = true,
                    incomingInvite = false,
                    awaitingResponse = false,
                    lastEvent = event
                )
                ListenTogetherEventType.REJECT,
                ListenTogetherEventType.END_SESSION -> ListenTogetherState()
                else -> ListenTogetherState(
                    sessionId = event.sessionId,
                    noteId = event.noteId,
                    peerKey = senderKey,
                    active = true,
                    incomingInvite = false,
                    awaitingResponse = false,
                    lastEvent = event
                )
            }
        }
    }

    private fun encodeEvent(event: ListenTogetherEvent): String {
        return JSONObject()
            .put("astraType", "music_sync")
            .put("version", 1)
            .put("sessionId", event.sessionId)
            .put("noteId", event.noteId)
            .put("eventType", event.eventType.name)
            .put("positionMs", event.positionMs)
            .put("sentAt", event.sentAt)
            .toString()
    }
}
