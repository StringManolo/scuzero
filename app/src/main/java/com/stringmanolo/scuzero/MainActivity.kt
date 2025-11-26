package com.stringmanolo.scuzero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class MainActivity : AppCompatActivity() {
  private lateinit var cameraManager: CameraManager

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val webView = WebView(this)
    setContentView(webView)

    cameraManager = CameraManager(this)

    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.addJavascriptInterface(WebAppInterface(this, cameraManager), "scuzero")
    webView.loadUrl("file:///android_asset/index.html")
  }
}

class CameraManager(private val context: Context) {
  private val devicePolicyManager: DevicePolicyManager =
  context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
  private val adminComponentName: ComponentName =
  ComponentName(context, MyDeviceAdminReceiver::class.java)

  fun setCameraEnabled(enabled: Boolean): String {
    return try {
      if (!devicePolicyManager.isAdminActive(adminComponentName)) {
        requestAdminPermissions()
        return "admin_required"
      }
      devicePolicyManager.setCameraDisabled(adminComponentName, !enabled)
      if (enabled) "camera_enabled" else "camera_disabled"
    } catch (e: SecurityException) {
      "Security exception: Check permissions"
    } catch (e: IllegalStateException) {
      "Illegal state: Device policy not available"
    } catch (e: Exception) {
      "Error: ${e.message}"
    }
  }

  fun isCameraEnabled(): Boolean {
    return try {
      if (!devicePolicyManager.isAdminActive(adminComponentName)) {
        true
      } else {
        !devicePolicyManager.getCameraDisabled(adminComponentName)
      }
    } catch (e: Exception) {
      true
    }
  }

  fun isAdminActive(): Boolean {
    return try {
      devicePolicyManager.isAdminActive(adminComponentName)
    } catch (e: Exception) {
      false
    }
  }

  private fun requestAdminPermissions() {
    try {
      val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permissions needed to control the camera.")
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to request admin permissions", Toast.LENGTH_LONG).show()
    }
  }
}

class WebAppInterface(
  private val context: android.content.Context,
  private val cameraManager: CameraManager
) {
  @JavascriptInterface
  fun showToast(message: String) {
    try {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
    }
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    return "Android ${android.os.Build.VERSION.RELEASE}"
  }

  @JavascriptInterface
  fun enableCamera(): String {
    return try {
      cameraManager.setCameraEnabled(true)
    } catch (e: Exception) {
      "Exception: ${e.message}"
    }
  }

  @JavascriptInterface
  fun disableCamera(): String {
    return try {
      cameraManager.setCameraEnabled(false)
    } catch (e: Exception) {
      "Exception: ${e.message}"
    }
  }

  @JavascriptInterface
  fun isCameraEnabled(): Boolean {
    return try {
      cameraManager.isCameraEnabled()
    } catch (e: Exception) {
      true
    }
  }

  @JavascriptInterface
  fun isAdminActive(): Boolean {
    return try {
      cameraManager.isAdminActive()
    } catch (e: Exception) {
      false
    }
  }

  @JavascriptInterface
  fun getCameraStatus(): String {
    return try {
      when {
        !cameraManager.isAdminActive() -> "admin_required"
        cameraManager.isCameraEnabled() -> "enabled"
        else -> "disabled"
      }
    } catch (e: Exception) {
      "error"
    }
  }
}

class MyDeviceAdminReceiver : android.app.admin.DeviceAdminReceiver()
