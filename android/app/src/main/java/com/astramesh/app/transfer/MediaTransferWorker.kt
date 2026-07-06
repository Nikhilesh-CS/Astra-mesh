package com.astramesh.app.transfer

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astramesh.app.data.AppDatabase
import com.astramesh.app.data.TransferStatus
import com.astramesh.app.network.MessageRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.Base64

class MediaTransferWorker(
    context: Context,
    params: WorkerParameters,
    private val db: AppDatabase,
    private val messageRouter: MessageRouter
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_USE_WIFI_DIRECT = "use_wifi_direct"
        private const val TAG = "MediaTransferWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return@withContext Result.failure()
        val useWifiDirect = inputData.getBoolean(KEY_USE_WIFI_DIRECT, false)

        val message = db.messageDao().getMessageById(messageId) ?: return@withContext Result.failure()
        val transfer = db.mediaTransferDao().getTransferSync(messageId) ?: return@withContext Result.failure()
        val contact = db.contactDao().getContact(message.contactKey) ?: return@withContext Result.failure()

        if (message.localUri == null) return@withContext Result.failure()
        val file = File(message.localUri)
        if (!file.exists()) {
            db.mediaTransferDao().updateStatus(messageId, TransferStatus.FAILED.name, System.currentTimeMillis())
            return@withContext Result.failure()
        }

        db.mediaTransferDao().updateStatus(messageId, TransferStatus.SENDING.name, System.currentTimeMillis())

        val transport = messageRouter.getBestTransport(contact)
        val chunkSize = when (transport) {
            com.astramesh.app.network.Transport.NEARBY_DIRECT -> 128 * 1024
            com.astramesh.app.network.Transport.NEARBY_RELAY -> 128 * 1024
            com.astramesh.app.network.Transport.TOR -> 32 * 1024
            else -> 32 * 1024
        }
        
        val finalChunkSize = if (useWifiDirect) 512 * 1024 else chunkSize
        val windowSize = if (transport == com.astramesh.app.network.Transport.TOR) 4 else 8
        
        val fileSize = file.length()
        val totalChunks = kotlin.math.ceil(fileSize.toDouble() / finalChunkSize).toInt()

        if (transfer.totalChunks != totalChunks) {
            db.openHelper.writableDatabase.execSQL("UPDATE media_transfers SET totalChunks = ? WHERE messageId = ?", arrayOf(totalChunks, messageId))
        }

        val startTime = System.currentTimeMillis()
        Log.i(TAG, "Transfer Started\nTransport: ${if(useWifiDirect) "Wi-Fi Direct" else transport.name}\nFile Size: $fileSize\nChunk Size: $finalChunkSize\nTotal Chunks: $totalChunks")

        try {
            RandomAccessFile(file, "r").use { raf ->
                var chunkIndex = transfer.completedChunks
                
                while (chunkIndex < totalChunks) {
                    val batchSize = minOf(windowSize, totalChunks - chunkIndex)
                    val deferredList = mutableListOf<kotlinx.coroutines.Deferred<Boolean>>()
                    
                    for (i in 0 until batchSize) {
                        val currentChunkIndex = chunkIndex + i
                        deferredList.add(async(Dispatchers.Default) {
                            val buffer = ByteArray(finalChunkSize)
                            var actualChunk: ByteArray
                            synchronized(raf) {
                                raf.seek((currentChunkIndex * finalChunkSize).toLong())
                                val bytesRead = raf.read(buffer)
                                actualChunk = if (bytesRead == finalChunkSize) buffer else buffer.copyOf(maxOf(0, bytesRead))
                            }
                            
                            val base64Chunk = Base64.getEncoder().encodeToString(actualChunk)
                            
                            val chunkPayload = JSONObject().apply {
                                put("msgId", messageId)
                                put("chunkIndex", currentChunkIndex)
                                put("totalChunks", totalChunks)
                                put("data", base64Chunk)
                                if (currentChunkIndex == 0) {
                                    put("mimeType", message.mimeType)
                                    put("messageType", message.messageType)
                                    put("fileName", message.fileName)
                                    put("checksum", message.checksum)
                                    put("fileSize", message.fileSize)
                                }
                            }.toString()
                            
                            val result = messageRouter.sendRawPayload(message.contactKey, chunkPayload, com.astramesh.app.network.MeshProtocol.TYPE_MEDIA_CHUNK)
                            result.success
                        })
                    }
                    
                    val results = deferredList.map { it.await() }
                    if (results.any { !it }) {
                        db.mediaTransferDao().updateStatus(messageId, TransferStatus.RETRYING.name, System.currentTimeMillis())
                        return@withContext Result.retry()
                    }
                    
                    chunkIndex += batchSize
                    
                    // DB Optimization: Update every 5% or 10 chunks or at end
                    val progressPercent = ((chunkIndex).toFloat() / totalChunks * 100).toInt()
                    val prevPercent = (((chunkIndex - batchSize).toFloat() / totalChunks) * 100).toInt()
                    
                    if (chunkIndex % 10 == 0 || chunkIndex == totalChunks || (progressPercent - prevPercent >= 5)) {
                        db.mediaTransferDao().updateProgress(messageId, chunkIndex, TransferStatus.SENDING.name, System.currentTimeMillis())
                        db.openHelper.writableDatabase.execSQL("UPDATE messages SET transferProgress = ? WHERE messageId = ?", arrayOf(progressPercent, messageId))
                    }
                }
            }

            val transferTime = (System.currentTimeMillis() - startTime) / 1000.0
            Log.i(TAG, "Transfer Completed\nTransfer Time: $transferTime seconds")

            db.mediaTransferDao().updateStatus(messageId, TransferStatus.COMPLETED.name, System.currentTimeMillis())
            db.messageDao().updateMessageStatus(messageId, "sent")
            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Transfer failed", e)
            db.mediaTransferDao().updateStatus(messageId, TransferStatus.FAILED.name, System.currentTimeMillis())
            return@withContext Result.failure()
        }
    }
}
