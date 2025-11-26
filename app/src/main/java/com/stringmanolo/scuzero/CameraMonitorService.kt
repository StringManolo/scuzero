package com.stringmanolo.scuzero

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CameraMonitorService : Service() {
  private val usageStatsManager by lazy { 
    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager 
  }
  private val cameraManager by lazy {
    getSystemService(Context.CAMERA_SERVICE) as CameraManager
  }
  private val notificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val cameraAccessLogs = ConcurrentLinkedQueue<String>()
  private var isMonitoring = false
  private val checkInterval = 1000L // 1 second for more responsive detection

  companion object {
    const val NOTIFICATION_ID = 12346
    const val CHANNEL_ID = "camera_monitor_channel"
    const val ALERT_CHANNEL_ID = "camera_alert_channel"
    var latestLogs = ConcurrentLinkedQueue<String>()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, createMonitoringNotification())
    startAdvancedMonitoring()
    return START_STICKY
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val monitorChannel = NotificationChannel(
        CHANNEL_ID,
        "Camera Privacy Protection",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors camera access continuously"
      }

      val alertChannel = NotificationChannel(
        ALERT_CHANNEL_ID,
        "Camera Access Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts when camera is accessed"
        enableVibration(true)
        enableLights(true)
      }

      notificationManager.createNotificationChannel(monitorChannel)
      notificationManager.createNotificationChannel(alertChannel)
    }
  }

  private fun createMonitoringNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Camera Privacy Protection")
    .setContentText("Monitoring camera access...")
    .setSmallIcon(android.R.drawable.ic_lock_lock)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .build()
  }

  private fun createAlertNotification(appName: String, packageName: String): Notification {
    return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
    .setContentTitle("🚨 Camera Access Detected")
    .setContentText("$appName is using the camera")
    .setSmallIcon(android.R.drawable.ic_dialog_alert)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setAutoCancel(true)
    .setStyle(NotificationCompat.BigTextStyle()
    .bigText("Application: $appName\nPackage: $packageName\nTime: ${getCurrentTime()}\n\nCamera privacy may be compromised."))
    .build()
  }

  private fun startAdvancedMonitoring() {
    if (isMonitoring) return

    isMonitoring = true
    Thread {
      val knownCameraApps = setOf(
        "camera",
        "com.android.camera",
        "com.google.android.GoogleCamera", 
        "com.sec.android.app.camera",
        "com.huawei.camera",
        "com.oneplus.camera",
        "com.miui.camera",
        "com.coloros.camera",
        "com.realme.camera",
        "com.vivo.camera"
      )

      val previousForegroundApps = mutableSetOf<String>()
      var cameraStateChangeCount = 0
      var lastCameraStateCheck = System.currentTimeMillis()

      while (isMonitoring) {
        try {
          val currentTime = System.currentTimeMillis()

          if (currentTime - lastCameraStateCheck > 500) {
            checkCameraStateChanges()
            lastCameraStateCheck = currentTime
          }

          val foregroundApp = getForegroundApp()
          if (foregroundApp != null && !previousForegroundApps.contains(foregroundApp)) {
            if (isCameraRelatedApp(foregroundApp, knownCameraApps)) {
              val appName = getAppName(foregroundApp)
              val detailedInfo = getAppDetailedInfo(foregroundApp)
              val logEntry = createDetailedLogEntry("FOREGROUND_APP", appName, foregroundApp, detailedInfo)

              addLogEntry(logEntry)
              showCameraAlert(appName, foregroundApp, "App brought to foreground")
            }
          }

          previousForegroundApps.clear()
          if (foregroundApp != null) {
            previousForegroundApps.add(foregroundApp)
          }

          checkUsageEvents()

          Thread.sleep(checkInterval)
        } catch (e: InterruptedException) {
          break
        } catch (e: Exception) {
          continue
        }
      }
    }.start()
  }

  private fun checkCameraStateChanges() {
    try {
      cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
          val logEntry = createDetailedLogEntry("TORCH_EVENT", "System", cameraId, "Torch ${if (enabled) "enabled" else "disabled"}")
          addLogEntry(logEntry)
          showCameraAlert("System", cameraId, "Flashlight toggled")
        }
      }, null)
    } catch (e: SecurityException) {
    } catch (e: Exception) {
    }
  }

  private fun checkUsageEvents() {
    try {
      val endTime = System.currentTimeMillis()
      val beginTime = endTime - checkInterval

      val events = usageStatsManager.queryEvents(beginTime, endTime)
      val event = UsageEvents.Event()

      while (events.hasNextEvent()) {
        events.getNextEvent(event)

        when (event.eventType) {
          UsageEvents.Event.ACTIVITY_RESUMED -> {
            if (isCameraRelatedApp(event.packageName, emptySet())) {
              val appName = getAppName(event.packageName)
              val detailedInfo = getAppDetailedInfo(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_RESUMED", appName, event.packageName, detailedInfo)

              addLogEntry(logEntry)
              showCameraAlert(appName, event.packageName, "Activity resumed")
            }
          }
          UsageEvents.Event.ACTIVITY_PAUSED -> {
            if (isCameraRelatedApp(event.packageName, emptySet())) {
              val appName = getAppName(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_PAUSED", appName, event.packageName, "App backgrounded")
              addLogEntry(logEntry)
            }
          }
        }
      }
    } catch (e: SecurityException) {
    } catch (e: Exception) {
    }
  }

  private fun getForegroundApp(): String? {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val usageStatsList = usageStatsManager.queryUsageStats(
          UsageStatsManager.INTERVAL_DAILY,
          System.currentTimeMillis() - 1000,
          System.currentTimeMillis()
        )

        usageStatsList?.maxByOrNull { it.lastTimeUsed }?.packageName
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun isCameraRelatedApp(packageName: String, knownCameraApps: Set<String>): Boolean {
    return knownCameraApps.any { packageName.contains(it, ignoreCase = true) } ||
    packageName.contains("camera", ignoreCase = true) ||
    hasCameraPermission(packageName)
  }

  private fun hasCameraPermission(packageName: String): Boolean {
    return try {
      val packageManager = applicationContext.packageManager
      val permissionCheck = packageManager.checkPermission(
        android.Manifest.permission.CAMERA, 
        packageName
      )
      permissionCheck == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
      false
    }
  }

  private fun getAppName(packageName: String): String {
    return try {
      val packageManager = applicationContext.packageManager
      val appInfo = packageManager.getApplicationInfo(packageName, 0)
      packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
      packageName
    }
  }

  private fun getAppDetailedInfo(packageName: String): String {
    return try {
      val packageManager = applicationContext.packageManager
      val appInfo = packageManager.getApplicationInfo(packageName, 0)
      val permissions = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
      .requestedPermissions?.filter { 
        it.contains("CAMERA") || it.contains("RECORD") 
      }?.joinToString(", ") ?: "No specific camera permissions"

      """
      App: ${packageManager.getApplicationLabel(appInfo)}
      Package: $packageName
      Permissions: $permissions
      UID: ${appInfo.uid}
      Install Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(packageManager.getPackageInfo(packageName, 0).firstInstallTime))}
      """.trimIndent()
    } catch (e: Exception) {
      "Detailed info unavailable"
    }
  }

  private fun createDetailedLogEntry(eventType: String, appName: String, packageName: String, details: String): String {
    val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    return """
    [${getCurrentTime()}] 
    🚨 CAMERA ACCESS DETECTED 🚨
    Event: $eventType
    Application: $appName
    Package: $packageName
    Timestamp: $timestamp

    Detailed Information:
    $details

    Stack Trace Context:
    ${Thread.currentThread().stackTrace.take(10).joinToString("\n") { 
      "  at ${it.className}.${it.methodName} (${it.fileName}:${it.lineNumber})" 
    }}
    """.trimIndent()
  }

  private fun addLogEntry(logEntry: String) {
    cameraAccessLogs.add(logEntry)
    latestLogs.add(logEntry)

    if (cameraAccessLogs.size > 20) {
      cameraAccessLogs.poll()
    }
    if (latestLogs.size > 20) {
      latestLogs.poll()
    }
  }

  private fun showCameraAlert(appName: String, packageName: String, reason: String) {
    showToast("📸 Camera accessed by: $appName")
    showAlertNotification(appName, packageName, reason)
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  private fun showAlertNotification(appName: String, packageName: String, reason: String) {
    val alertId = System.currentTimeMillis().toInt()
    val notification = createAlertNotification(appName, packageName)
    notificationManager.notify(alertId, notification)
  }

  private fun getCurrentTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
  }

  fun getLogs(): List<String> {
    return cameraAccessLogs.toList().reversed()
  }

  override fun onDestroy() {
    isMonitoring = false
    super.onDestroy()
  }
}
