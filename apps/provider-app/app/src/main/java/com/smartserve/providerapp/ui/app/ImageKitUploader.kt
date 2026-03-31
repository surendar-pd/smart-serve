package com.smartserve.providerapp.ui.app

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Minimal ImageKit uploader using the ImageKit Upload API.
 * WARNING: This variant uses the ImageKit private key on-device (insecure).
 */
class ImageKitUploader(
    private val context: Context,
    private val http: OkHttpClient = OkHttpClient(),
) {
    fun upload(
        publicKey: String,
        privateKey: String,
        fileUri: Uri,
        folder: String,
        fileName: String,
    ): String {
        val tmp = materializeToTempFile(fileUri, fileName)
        val mime = context.contentResolver.getType(fileUri) ?: "image/jpeg"
        val uploadUrl = "https://upload.imagekit.io/api/v1/files/upload"

        val filePart = tmp.asRequestBody(mime.toMediaTypeOrNull())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", tmp.name, filePart)
            .addFormDataPart("fileName", tmp.name)
            .addFormDataPart("folder", folder)
            .addFormDataPart("publicKey", publicKey)
            .build()

        val basic = Base64.getEncoder().encodeToString("$privateKey:".toByteArray())
        val req = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", "Basic $basic")
            .post(multipart)
            .build()
        http.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) error("ImageKit upload failed: HTTP ${res.code} $body")
            val json = JSONObject(body)
            return json.optString("url").ifBlank { error("ImageKit upload succeeded but returned no url") }
        }
    }

    private fun materializeToTempFile(uri: Uri, fileName: String): File {
        val tmp = File(context.cacheDir, "ik_${UUID.randomUUID()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { out -> input.copyTo(out) }
        } ?: error("Could not open image uri: $uri")
        return tmp
    }
}

