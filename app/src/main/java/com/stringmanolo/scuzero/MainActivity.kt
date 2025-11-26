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
import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
  private lateinit var cameraMonitor: CameraMonitorService
  private lateinit var microphoneMonitor: MicrophoneMonitorService
  private lateinit var gpsMonitor: GpsMonitorService
  private lateinit var internetMonitor: InternetMonitorService

  private val CAMERA_PERMISSION_REQUEST_CODE = 100
  private val STORAGE_PERMISSION_REQUEST_CODE = 101
  private val MICROPHONE_PERMISSION_REQUEST_CODE = 102
  private val LOCATION_PERMISSION_REQUEST_CODE = 103

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val webView = WebView(this)
    setContentView(webView)

    cameraMonitor = CameraMonitorService()
    microphoneMonitor = MicrophoneMonitorService()
    gpsMonitor = GpsMonitorService()
    internetMonitor = InternetMonitorService()

    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.addJavascriptInterface(WebAppInterface(this, cameraMonitor, microphoneMonitor, gpsMonitor, internetMonitor), "scuzero")
    webView.loadUrl("file:///android_asset/index.html")

    checkPermissions()
  }

  private fun checkPermissions() {
    val permissionsToRequest = mutableListOf<String>()

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.CAMERA)
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
    != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
    != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
    != PackageManager.PERMISSION_GRANTED) {
      permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(
        this,
        permissionsToRequest.toTypedArray(),
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
        for (i in permissions.indices) {
          when (permissions[i]) {
            Manifest.permission.CAMERA -> {
              if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, "Camera permission denied - some features may not work", Toast.LENGTH_LONG).show()
              }
            }
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
              if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, "Storage permission denied - logs won't be saved", Toast.LENGTH_LONG).show()
              }
            }
            Manifest.permission.RECORD_AUDIO -> {
              if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, "Microphone permission denied - some features may not work", Toast.LENGTH_LONG).show()
              }
            }
            Manifest.permission.ACCESS_FINE_LOCATION -> {
              if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(this, "Location permission denied - some features may not work", Toast.LENGTH_LONG).show()
              }
            }
          }
        }
      }
    }
  }
}

class WebAppInterface(
  private val context: android.content.Context,
  private val cameraMonitor: CameraMonitorService,
  private val microphoneMonitor: MicrophoneMonitorService,
  private val gpsMonitor: GpsMonitorService,
  private val internetMonitor: InternetMonitorService
) {
  @JavascriptInterface
  fun showToast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    return "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
  }

  // Camera Monitor Functions
  @JavascriptInterface
  fun startCameraMonitoring(): String {
    return try {
      if (hasUsageStatsPermission()) {
        val intent = Intent(context, CameraMonitorService::class.java)
        context.startService(intent)
        "camera_monitoring_started"
      } else {
        "usage_permission_required"
      }
    } catch (e: Exception) {
      "camera_monitoring_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun stopCameraMonitoring(): String {
    return try {
      val intent = Intent(context, CameraMonitorService::class.java)
      context.stopService(intent)
      "camera_monitoring_stopped"
    } catch (e: Exception) {
      "camera_stop_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun getCameraAccessLogs(): String {
    return try {
      CameraMonitorService.latestLogs.joinToString("\n\n" + "=".repeat(50) + "\n\n")
    } catch (e: Exception) {
      "Error retrieving camera logs: ${e.message}"
    }
  }

  @JavascriptInterface
  fun clearCameraLogs(): String {
    return try {
      CameraMonitorService.latestLogs.clear()
      "camera_logs_cleared"
    } catch (e: Exception) {
      "camera_clear_failed: ${e.message}"
    }
  }

  // Microphone Monitor Functions
  @JavascriptInterface
  fun startMicrophoneMonitoring(): String {
    return try {
      if (hasUsageStatsPermission()) {
        val intent = Intent(context, MicrophoneMonitorService::class.java)
        context.startService(intent)
        "microphone_monitoring_started"
      } else {
        "usage_permission_required"
      }
    } catch (e: Exception) {
      "microphone_monitoring_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun stopMicrophoneMonitoring(): String {
    return try {
      val intent = Intent(context, MicrophoneMonitorService::class.java)
      context.stopService(intent)
      "microphone_monitoring_stopped"
    } catch (e: Exception) {
      "microphone_stop_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun getMicrophoneAccessLogs(): String {
    return try {
      MicrophoneMonitorService.latestLogs.joinToString("\n\n" + "=".repeat(50) + "\n\n")
    } catch (e: Exception) {
      "Error retrieving microphone logs: ${e.message}"
    }
  }

  @JavascriptInterface
  fun clearMicrophoneLogs(): String {
    return try {
      MicrophoneMonitorService.latestLogs.clear()
      "microphone_logs_cleared"
    } catch (e: Exception) {
      "microphone_clear_failed: ${e.message}"
    }
  }

  // GPS Monitor Functions
  @JavascriptInterface
  fun startGpsMonitoring(): String {
    return try {
      if (hasUsageStatsPermission()) {
        val intent = Intent(context, GpsMonitorService::class.java)
        context.startService(intent)
        "gps_monitoring_started"
      } else {
        "usage_permission_required"
      }
    } catch (e: Exception) {
      "gps_monitoring_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun stopGpsMonitoring(): String {
    return try {
      val intent = Intent(context, GpsMonitorService::class.java)
      context.stopService(intent)
      "gps_monitoring_stopped"
    } catch (e: Exception) {
      "gps_stop_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun getGpsAccessLogs(): String {
    return try {
      GpsMonitorService.latestLogs.joinToString("\n\n" + "=".repeat(50) + "\n\n")
    } catch (e: Exception) {
      "Error retrieving gps logs: ${e.message}"
    }
  }

  @JavascriptInterface
  fun clearGpsLogs(): String {
    return try {
      GpsMonitorService.latestLogs.clear()
      "gps_logs_cleared"
    } catch (e: Exception) {
      "gps_clear_failed: ${e.message}"
    }
  }

  // Internet Monitor Functions
  @JavascriptInterface
  fun startInternetMonitoring(): String {
    return try {
      if (hasUsageStatsPermission()) {
        val intent = Intent(context, InternetMonitorService::class.java)
        context.startService(intent)
        "internet_monitoring_started"
      } else {
        "usage_permission_required"
      }
    } catch (e: Exception) {
      "internet_monitoring_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun stopInternetMonitoring(): String {
    return try {
      val intent = Intent(context, InternetMonitorService::class.java)
      context.stopService(intent)
      "internet_monitoring_stopped"
    } catch (e: Exception) {
      "internet_stop_failed: ${e.message}"
    }
  }

  @JavascriptInterface
  fun getInternetAccessLogs(): String {
    return try {
      InternetMonitorService.latestLogs.joinToString("\n\n" + "=".repeat(50) + "\n\n")
    } catch (e: Exception) {
      "Error retrieving internet logs: ${e.message}"
    }
  }

  @JavascriptInterface
  fun clearInternetLogs(): String {
    return try {
      InternetMonitorService.latestLogs.clear()
      "internet_logs_cleared"
    } catch (e: Exception) {
      "internet_clear_failed: ${e.message}"
    }
  }

  // Common Functions
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

@JavascriptInterface
fun copyToClipboard(text: String) {
  try {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Scuzero Logs", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
  } catch (e: Exception) {
    Toast.makeText(context, "Failed to copy logs", Toast.LENGTH_SHORT).show()
  }
}
}
