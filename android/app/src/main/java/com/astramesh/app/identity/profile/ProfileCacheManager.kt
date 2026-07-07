package com.astramesh.app.identity.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File

interface ProfileCacheManager {
    fun saveProfileAvatar(contactKey: String, avatarData: ByteArray): File
    fun getProfileAvatar(contactKey: String): File?
    fun saveThumbnail(contactKey: String, thumbnailData: ByteArray): File
    fun getThumbnail(contactKey: String): File?
    fun getAvatarBitmap(contactKey: String): Bitmap?
    fun getThumbnailBitmap(contactKey: String): Bitmap?
    fun clearCache(contactKey: String)
    fun clearAllCache()
}

class ProfileCacheManagerImpl(context: Context) : ProfileCacheManager {
    private val avatarDir = File(context.cacheDir, "avatars").apply { mkdirs() }
    private val thumbnailDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }

    // Memory cache for Bitmaps
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val avatarMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val thumbnailMemoryCache = object : LruCache<String, Bitmap>(cacheSize / 4) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    override fun saveProfileAvatar(contactKey: String, avatarData: ByteArray): File {
        val file = File(avatarDir, "$contactKey.webp")
        file.writeBytes(avatarData)
        // Update memory cache
        val bitmap = BitmapFactory.decodeByteArray(avatarData, 0, avatarData.size)
        if (bitmap != null) {
            avatarMemoryCache.put(contactKey, bitmap)
        }
        return file
    }

    override fun getProfileAvatar(contactKey: String): File? {
        val file = File(avatarDir, "$contactKey.webp")
        return if (file.exists()) file else null
    }

    override fun getAvatarBitmap(contactKey: String): Bitmap? {
        avatarMemoryCache.get(contactKey)?.let { return it }

        val file = getProfileAvatar(contactKey)
        if (file != null) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                avatarMemoryCache.put(contactKey, bitmap)
                return bitmap
            }
        }
        return null
    }

    override fun saveThumbnail(contactKey: String, thumbnailData: ByteArray): File {
        val file = File(thumbnailDir, "${contactKey}_thumb.webp")
        file.writeBytes(thumbnailData)
        val bitmap = BitmapFactory.decodeByteArray(thumbnailData, 0, thumbnailData.size)
        if (bitmap != null) {
            thumbnailMemoryCache.put(contactKey, bitmap)
        }
        return file
    }

    override fun getThumbnail(contactKey: String): File? {
        val file = File(thumbnailDir, "${contactKey}_thumb.webp")
        return if (file.exists()) file else null
    }

    override fun getThumbnailBitmap(contactKey: String): Bitmap? {
        thumbnailMemoryCache.get(contactKey)?.let { return it }

        val file = getThumbnail(contactKey)
        if (file != null) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                thumbnailMemoryCache.put(contactKey, bitmap)
                return bitmap
            }
        }
        return null
    }

    override fun clearCache(contactKey: String) {
        avatarMemoryCache.remove(contactKey)
        thumbnailMemoryCache.remove(contactKey)
        File(avatarDir, "$contactKey.webp").delete()
        File(thumbnailDir, "${contactKey}_thumb.webp").delete()
    }

    override fun clearAllCache() {
        avatarMemoryCache.evictAll()
        thumbnailMemoryCache.evictAll()
        avatarDir.listFiles()?.forEach { it.delete() }
        thumbnailDir.listFiles()?.forEach { it.delete() }
    }
}
