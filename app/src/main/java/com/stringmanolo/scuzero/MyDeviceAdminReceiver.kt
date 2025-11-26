package com.stringmanolo.scuzero

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
  override fun onEnabled(context: Context, intent: Intent) {
    super.onEnabled(context, intent)
  }

  override fun onDisabled(context: Context, intent: Intent) {
    Toast.makeText(context, "Disabling camera ...", Toast.LENGTH_SHORT).show()

    Handler(Looper.getMainLooper()).postDelayed({
      Toast.makeText(context, "2s delsy before disabling", Toast.LENGTH_LONG).show()
    }, 2000)

    try {
      super.onDisabled(context, intent)
      Toast.makeText(context, "Disabled", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Toast.makeText(context, "Error: ${e.toString()}", Toast.LENGTH_LONG).show()
    }
  }
}



/*package com.stringmanolo.scuzero

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
  override fun onEnabled(context: Context, intent: Intent) {
    super.onEnabled(context, intent)
  }

  override fun onDisabled(context: Context, intent: Intent) {
    super.onDisabled(context, intent)
  }
}*/
