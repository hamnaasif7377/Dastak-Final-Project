package com.DASTAK.i230613_i230658_i230736

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object FCMTokenService {

    private val client = OkHttpClient()
    private const val TAG = "FCMTokenService"

    /**
     * Get FCM token and save it to server
     */
    fun initializeFCM(context: Context) {
        Log.d(TAG, "=== Initializing FCM ===")

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "❌ Fetching FCM token FAILED", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "✅ FCM Token obtained successfully")
            Log.d(TAG, "Token: $token")

            // Save token to server
            saveTokenToServer(context, token)
        })
    }

    /**
     * Save FCM token to server
     */
    private fun saveTokenToServer(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        Log.d(TAG, "=== Saving Token to Server ===")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Token: $token")

        if (userId == -1) {
            Log.e(TAG, "❌ User not logged in, skipping token save")
            return
        }

        val formBody = FormBody.Builder()
            .add("user_id", userId.toString())
            .add("fcm_token", token)
            .build()

        val url = Constants.BASE_URL + "save_fcm_token.php"
        Log.d(TAG, "Sending to URL: $url")

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Network error while saving FCM token", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d(TAG, "=== Server Response ===")
                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseBody")

                try {
                    if (!responseBody.isNullOrEmpty()) {
                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.getString("status")
                        val message = jsonResponse.getString("message")

                        if (status == "success") {
                            Log.d(TAG, "✅ Token saved successfully: $message")
                        } else {
                            Log.e(TAG, "❌ Server error: $message")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing response", e)
                }
            }
        })
    }
}