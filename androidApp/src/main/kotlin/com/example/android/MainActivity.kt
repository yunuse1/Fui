package com.example.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple UI with TextView
        textView = TextView(this).apply {
            text = "Loading analysis..."
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }
        setContentView(textView)

        // Fetch analysis from server
        fetchAnalysis()
    }

    private fun fetchAnalysis() {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("http://10.0.2.2:8080/test-analyze") // Use 10.0.2.2 for localhost in emulator
                    .build()

                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                if (json.optBoolean("success", false)) {
                    val analysis = json.optJSONObject("analysis")?.toString(2) ?: "No analysis"
                    val elapsed = json.optLong("elapsed_ms", 0)
                    val cameraUrl = json.optString("camera_url", "")
                    textView.text = "Camera: $cameraUrl\nElapsed: ${elapsed}ms\nAnalysis:\n$analysis"
                } else {
                    textView.text = "Error: ${json.optString("error", "Unknown")}"
                }
            } catch (e: Exception) {
                textView.text = "Error: ${e.message}"
            }
        }
    }
}
