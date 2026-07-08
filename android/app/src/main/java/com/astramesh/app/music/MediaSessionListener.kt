package com.astramesh.app.music

import android.content.Context
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class MediaSessionListener(private val context: Context) {
    suspend fun detectCurrentTrack(): DetectedMusicTrack? = withContext(Dispatchers.Default) {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return@withContext null
        val controllers = runCatching { manager.getActiveSessions(listenerComponentName()) }.getOrElse { emptyList() }
        val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
            ?: return@withContext null
        controller.toDetectedTrack()
    }

    fun notificationAccessIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val component = listenerComponentName().flattenToString()
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }

    private fun listenerComponentName(): ComponentName {
        return ComponentName(context, MusicSessionNotificationListenerService::class.java)
    }

    private fun MediaController.toDetectedTrack(): DetectedMusicTrack? {
        val metadata = metadata ?: return null
        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        if (title.isBlank() && artist.isBlank()) return null
        val album = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
        val artUri = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI)
        val artBitmap = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        val mediaId = metadata.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?: "${packageName}:${title.lowercase()}:${artist.lowercase()}"
        return DetectedMusicTrack(
            trackId = mediaId,
            trackName = title.ifBlank { "Unknown track" },
            artist = artist.ifBlank { "Unknown artist" },
            album = album,
            albumArtUri = artUri?.let { Uri.parse(it).toString() } ?: artBitmap?.let { saveArtworkBitmap(mediaId, it) },
            provider = packageName,
            playbackPositionMs = playbackState?.position ?: 0L
        )
    }

    private fun saveArtworkBitmap(mediaId: String, bitmap: Bitmap): String? {
        return runCatching {
            val dir = File(context.filesDir, "music_art").apply { mkdirs() }
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(mediaId.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val file = File(dir, "$hash.webp")
            if (!file.exists()) {
                file.outputStream().use { stream ->
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 92, stream)
                }
            }
            file.absolutePath
        }.getOrNull()
    }
}
