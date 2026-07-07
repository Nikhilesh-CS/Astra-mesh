package com.astramesh.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ProcessedImage(
    val resolution: Int,
    val data: ByteArray
)

data class ImagePipelineResult(
    val avatarHash: String,
    val images: List<ProcessedImage>
)

class ImageProcessor(private val context: Context) {

    suspend fun processImage(uri: Uri): ImagePipelineResult = withContext(Dispatchers.IO) {
        val hash = computeSha256(uri)
        
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
        
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        
        if (originalWidth <= 0 || originalHeight <= 0) {
            throw IllegalArgumentException("Invalid image dimensions")
        }
        
        val maxOriginalDimension = max(originalWidth, originalHeight)
        
        // Define resolutions
        val targetSizes = listOf(
            min(maxOriginalDimension, 2048), // Cap original dimension at 2048 for memory safety
            512,
            256,
            96
        ).filter { it <= maxOriginalDimension || it == 96 }.distinct().sortedDescending()
        
        val processedImages = mutableListOf<ProcessedImage>()
        
        for (targetSize in targetSizes) {
            val processedImage = decodeAndCompress(uri, originalWidth, originalHeight, targetSize)
            if (processedImage != null) {
                processedImages.add(processedImage)
            }
        }
        
        ImagePipelineResult(
            avatarHash = hash,
            images = processedImages
        )
    }

    private fun computeSha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        } ?: throw IllegalArgumentException("Could not open input stream for URI")
        
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun decodeAndCompress(
        uri: Uri,
        originalWidth: Int,
        originalHeight: Int,
        targetSize: Int
    ): ProcessedImage? {
        val maxDim = max(originalWidth, originalHeight)
        val scale = if (maxDim > targetSize) maxDim / targetSize else 1
        
        val options = BitmapFactory.Options().apply {
            // inSampleSize needs to be a power of 2
            inSampleSize = if (scale > 1) Integer.highestOneBit(scale) else 1
            inJustDecodeBounds = false
        }
        
        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        } ?: return null
        
        val currentMaxDim = max(bitmap.width, bitmap.height)
        val finalBitmap = if (currentMaxDim > targetSize) {
            val ratio = targetSize.toFloat() / currentMaxDim
            val newWidth = (bitmap.width * ratio).roundToInt()
            val newHeight = (bitmap.height * ratio).roundToInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }
            scaledBitmap
        } else {
            bitmap
        }
        
        val outputStream = ByteArrayOutputStream()
        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        
        finalBitmap.compress(compressFormat, 80, outputStream)
        finalBitmap.recycle()
        
        return ProcessedImage(
            resolution = targetSize,
            data = outputStream.toByteArray()
        )
    }
}
