package com.smartserve.customerapp.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FcmApiService {

    // Your Firebase project ID
    private const val PROJECT_ID = "smart-serve-76d01"
    private const val FCM_URL =
        "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    suspend fun sendNotification(
        recipientToken: String,
        title: String,
        body: String,
        bookingId: String,
        accessToken: String,       // ← passed in from ViewModel
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(FCM_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", recipientToken)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })
                    put("data", JSONObject().apply {
                        put("bookingId", bookingId)
                        put("title", title)
                        put("body", body)
                    })
                    put("android", JSONObject().apply {
                        put("priority", "high")
                        put("notification", JSONObject().apply {
                            put("channel_id", "chat_messages")
                            put("sound", "default")
                        })
                    })
                })
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            Log.d("FCM", "Response code: $responseCode")
            connection.disconnect()

        } catch (e: Exception) {
            Log.e("FCM", "Failed to send notification: ${e.localizedMessage}")
        }
    }
}
