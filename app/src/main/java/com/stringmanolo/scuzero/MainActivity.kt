package com.stringmanolo.scuzero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast


import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/* debug */
import android.os.Handler
import android.os.Looper

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
    if (!devicePolicyManager.isAdminActive(adminComponentName)) {
      requestAdminPermissions()
      return "admin_required"
    }
    devicePolicyManager.setCameraDisabled(adminComponentName, !enabled)
    return if (enabled) "camera_enabled" else "camera_disabled"
  }

  fun isCameraEnabled(): Boolean {
    return if (!devicePolicyManager.isAdminActive(adminComponentName)) {
      true // Por defecto asumimos que está habilitada si no somos admin
    } else {
      !devicePolicyManager.getCameraDisabled(adminComponentName)
    }
  }

  fun isAdminActive(): Boolean {
    return devicePolicyManager.isAdminActive(adminComponentName)
  }

  private fun requestAdminPermissions() {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
      putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
      putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permissions needed to control the camers.")
    }
    context.startActivity(intent)
  }
}

class WebAppInterface(
  private val context: android.content.Context,
  private val cameraManager: CameraManager
) {

  /* General Interfaces */
  @JavascriptInterface
  fun showToast(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    return "Android ${android.os.Build.VERSION.RELEASE}"
  }

  
  /* Camera Interfaces */
  @JavascriptInterface
  fun enableCamera(): String {
    return cameraManager.setCameraEnabled(true)
  }

  @JavascriptInterface
  fun disableCamera(): String {
    Toast.makeText(context, "Disabling camera called ...", Toast.LENGTH_SHORT).show()

    Handler(Looper.getMainLooper()).postDelayed({
      Toast.makeText(context, "2s delay after disable camera called", Toast.LENGTH_LONG).show()
    }, 2000)

    try {
      cameraManager.setCameraEnabled(false)
      Toast.makeText(context, "Disabled", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Error: ${e.toString()}", Toast.LENGTH_LONG).show()
    }


    // return cameraManager.setCameraEnabled(false)
    return "Done."
  }

  @JavascriptInterface
  fun isCameraEnabled(): Boolean {
    return cameraManager.isCameraEnabled()
  }

  @JavascriptInterface
  fun isAdminActive(): Boolean {
    return cameraManager.isAdminActive()
  }

  @JavascriptInterface
  fun getCameraStatus(): String {
    return when {
      !cameraManager.isAdminActive() -> "admin_required"
      cameraManager.isCameraEnabled() -> "enabled"
      else -> "disabled"
    }
  }
}
