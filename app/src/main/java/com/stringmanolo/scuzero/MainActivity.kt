package com.stringmanolo.scuzero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import android.content.Intent
import android.content.Context

class MainActivity : AppCompatActivity() {
  private lateinit var cameraMonitor: CameraMonitorService

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val webView = WebView(this)
    setContentView(webView)

    cameraMonitor = CameraMonitorService()

    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.addJavascriptInterface(WebAppInterface(this, cameraMonitor), "scuzero")
    webView.loadUrl("file:///android_asset/index.html")
  }
}

class WebAppInterface(
  private val context: android.content.Context,
  private val cameraMonitor: CameraMonitorService
) {
  @JavascriptInterface
  fun showToast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    return "Android ${android.os.Build.VERSION.RELEASE}"
  }

  @JavascriptInterface
  fun startCameraMonitoring(): String {
    return try {
      val intent = Intent(context, CameraMonitorService::class.java)
      context.startService(intent)
      "monitoring_started"
    } catch (e: Exception) {
      "monitoring_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun stopCameraMonitoring(): String {
    return try {
      val intent = Intent(context, CameraMonitorService::class.java)
      context.stopService(intent)
      "monitoring_stopped"
    } catch (e: Exception) {
      "stop_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun getCameraAccessLogs(): String {
    return try {
      CameraMonitorService.latestLogs.joinToString("\n")
    } catch (e: Exception) {
      "Error retrieving logs"
    }
  }

  @JavascriptInterface
  fun clearCameraLogs(): String {
    return try {
      CameraMonitorService.latestLogs.clear()
      "logs_cleared"
    } catch (e: Exception) {
      "clear_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun openUsageAccessSettings(): String {
    return try {
      val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
      context.startActivity(intent)
      "opened_usage_settings"
    } catch (e: Exception) {
      "failed_to_open_settings"
    }
  }
}
