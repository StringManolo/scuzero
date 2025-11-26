package com.stringmanolo.scuzero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
  private lateinit var cameraMonitor: CameraMonitorService
  private val CAMERA_PERMISSION_REQUEST_CODE = 100

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

    checkCameraPermission()
  }

  private fun checkCameraPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        CAMERA_PERMISSION_REQUEST_CODE
      )
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      CAMERA_PERMISSION_REQUEST_CODE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, "Camera permission denied - some features may not work", Toast.LENGTH_LONG).show()
        }
      }
    }
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
    return "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
  }

  @JavascriptInterface
  fun startCameraMonitoring(): String {
    return try {
      if (hasUsageStatsPermission()) {
        val intent = Intent(context, CameraMonitorService::class.java)
        context.startService(intent)
        "monitoring_started"
      } else {
        "usage_permission_required"
      }
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
      CameraMonitorService.latestLogs.joinToString("\n\n" + "=".repeat(50) + "\n\n")
    } catch (e: Exception) {
      "Error retrieving logs: ${e.message}"
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

  @JavascriptInterface
  fun hasUsageStatsPermission(): Boolean {
    return try {
      val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
      val mode = appOps.checkOpNoThrow(
        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
      )
      mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
      false
    }
  }

  @JavascriptInterface
  fun requestCameraPermission(): String {
    return try {
      if (androidx.core.content.ContextCompat.checkSelfPermission(
        context, 
        Manifest.permission.CAMERA
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      val intent = Intent(context, MainActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      "camera_permission_requested"
    } else {
      "camera_permission_already_granted"
    }
  } catch (e: Exception) {
    "permission_request_failed: ${e.message}"
  }
}
}
