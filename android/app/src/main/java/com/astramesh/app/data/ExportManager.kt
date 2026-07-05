package com.astramesh.app.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ExportManager {

    suspend fun exportChatsToJSON(context: Context, db: AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val messages = db.messageDao().getAllMessagesSync()
            val jsonArray = JSONArray()

            for (msg in messages) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("messageId", msg.messageId)
                    put("contactKey", msg.contactKey)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("direction", msg.direction)
                    put("status", msg.status)
                    put("transport", msg.transport)
                }
                jsonArray.put(obj)
            }

            val file = File(context.cacheDir, "AstraMesh_Export_${System.currentTimeMillis()}.json")
            file.writeText(jsonArray.toString(4))

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "Export Chats")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ExportManager", "Failed to export chats", e)
            Result.failure(e)
        }
    }
}
