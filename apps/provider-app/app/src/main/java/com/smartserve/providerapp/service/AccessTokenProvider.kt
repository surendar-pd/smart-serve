package com.smartserve.providerapp.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64

object AccessTokenProvider {

    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            // Read service account JSON from assets
            val json = context.assets.open("service-account.json")
                .bufferedReader().use { it.readText() }
            val sa = JSONObject(json)

            val clientEmail = sa.getString("client_email")
            val privateKeyStr = sa.getString("private_key")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replace("\n", "")
                .trim()

            // Build JWT
            val now = System.currentTimeMillis() / 1000
            val header = Base64.encodeToString(
                """{"alg":"RS256","typ":"JWT"}""".toByteArray(),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val claims = Base64.encodeToString(
                """{"iss":"$clientEmail","scope":"$SCOPE","aud":"$TOKEN_URL","exp":${now + 3600},"iat":$now}""".toByteArray(),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val signingInput = "$header.$claims"

            // Sign JWT with private key
            val keyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            val signature = Signature.getInstance("SHA256withRSA").apply {
                initSign(privateKey)
                update(signingInput.toByteArray())
            }.sign()
            val signatureStr = Base64.encodeToString(
                signature,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val jwt = "$signingInput.$signatureStr"

            // Exchange JWT for access token
            val url = URL(TOKEN_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true

            val body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
            OutputStreamWriter(connection.outputStream).use { it.write(body) }

            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .use { it.readText() }
            connection.disconnect()

            JSONObject(response).getString("access_token")

        } catch (e: Exception) {
            Log.e("FCM", "Failed to get access token: ${e.localizedMessage}")
            null
        }
    }
}