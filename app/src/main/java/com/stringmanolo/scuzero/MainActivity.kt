package com.stringmanolo.scuzero

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
  private lateinit var cameraBlocker: AdvancedCameraBlocker
  
  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val webView = WebView(this)
    setContentView(webView)

    cameraBlocker = AdvancedCameraBlocker(this)

    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.addJavascriptInterface(WebAppInterface(this, cameraBlocker), "scuzero")
    webView.loadUrl("file:///android_asset/index.html")
  }
}

class WebAppInterface(
  private val context: android.content.Context,
  private val cameraBlocker: AdvancedCameraBlocker
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
  fun enableAdvancedCameraBlock(): String {
    return cameraBlocker.enableCompleteCameraBlock()
  }

  @JavascriptInterface
  fun disableAdvancedCameraBlock(): String {
    return cameraBlocker.disableCameraBlock()
  }

  @JavascriptInterface
  fun getCameraBlockStatus(): String {
    return "advanced_protection_available"
  }
}

class MyDeviceAdminReceiver : android.app.admin.DeviceAdminReceiver()
