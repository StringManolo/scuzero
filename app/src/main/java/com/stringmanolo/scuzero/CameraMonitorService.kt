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
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
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
  private val checkInterval = 1000L
  private val ourPackageName = "com.stringmanolo.scuzero"
  private val ourPackageName2 ="com.stringmanolo.scuzero.debug"

  // Improved duplicate detection
  private data class LogKey(val appName: String, val packageName: String, val eventType: String, val minute: String)
  private val recentLogs = LinkedHashMap<LogKey, Long>() // Track recent logs with timestamp
  private val duplicateWindow = 60000L // 1 minute in milliseconds

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
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
    .setContentTitle("📸 Camera Access Detected")
    .setContentText("$appName is using the camera")
    .setSmallIcon(android.R.drawable.ic_dialog_alert)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setAutoCancel(true)
    .setStyle(NotificationCompat.BigTextStyle()
    .bigText("Application: $appName\nPackage: $packageName\nTime: $timestamp\n\nOpen Scuzero to view detailed logs."))
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
      var lastCameraStateCheck = System.currentTimeMillis()

      while (isMonitoring) {
        try {
          val currentTime = System.currentTimeMillis()

          // Clean up old entries from recentLogs
          cleanOldRecentLogs(currentTime)

          if (currentTime - lastCameraStateCheck > 500) {
            checkCameraStateChanges(currentTime)
            lastCameraStateCheck = currentTime
          }

          val foregroundApp = getForegroundApp()
          if (foregroundApp != null && !previousForegroundApps.contains(foregroundApp)) {
            if (foregroundApp != ourPackageName && foregroundApp != ourPackageName2 && isCameraRelatedApp(foregroundApp, knownCameraApps)) {
              val appName = getAppName(foregroundApp)
              val detailedInfo = getAppDetailedInfo(foregroundApp)
              val logEntry = createDetailedLogEntry("FOREGROUND_APP", appName, foregroundApp, detailedInfo)

              if (shouldAddLog("FOREGROUND_APP", appName, foregroundApp, currentTime)) {
                addLogEntry(logEntry)
                showCameraAlert(appName, foregroundApp)
              }
            }
          }

          previousForegroundApps.clear()
          if (foregroundApp != null) {
            previousForegroundApps.add(foregroundApp)
          }

          checkUsageEvents(currentTime)

          Thread.sleep(checkInterval)
        } catch (e: InterruptedException) {
          break
        } catch (e: Exception) {
          continue
        }
      }
    }.start()
  }

  private fun cleanOldRecentLogs(currentTime: Long) {
    val iterator = recentLogs.iterator()
    while (iterator.hasNext()) {
      val (key, timestamp) = iterator.next()
      if (currentTime - timestamp > duplicateWindow) {
        iterator.remove()
      }
    }
  }

  private fun checkCameraStateChanges(currentTime: Long) {
    try {
      cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
          if (shouldIgnoreOurApp()) return

          val logEntry = createDetailedLogEntry("TORCH_EVENT", "System", cameraId, "Torch ${if (enabled) "enabled" else "disabled"}")

          if (shouldAddLog("TORCH_EVENT", "System", cameraId, currentTime)) {
            addLogEntry(logEntry)
            showCameraAlert("System", cameraId)
          }
        }
      }, null)
    } catch (e: SecurityException) {
    } catch (e: Exception) {
    }
  }

  private fun checkUsageEvents(currentTime: Long) {
    try {
      val endTime = System.currentTimeMillis()
      val beginTime = endTime - checkInterval

      val events = usageStatsManager.queryEvents(beginTime, endTime)
      val event = UsageEvents.Event()

      while (events.hasNextEvent()) {
        events.getNextEvent(event)

        if (event.packageName == ourPackageName ) continue
        if (event.packageName == ourPackageName2 ) continue

        when (event.eventType) {
          UsageEvents.Event.ACTIVITY_RESUMED -> {
            if (isCameraRelatedApp(event.packageName, emptySet())) {
              val appName = getAppName(event.packageName)
              val detailedInfo = getAppDetailedInfo(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_RESUMED", appName, event.packageName, detailedInfo)

              if (shouldAddLog("ACTIVITY_RESUMED", appName, event.packageName, currentTime)) {
                addLogEntry(logEntry)
                showCameraAlert(appName, event.packageName)
              }
            }
          }
          UsageEvents.Event.ACTIVITY_PAUSED -> {
            if (isCameraRelatedApp(event.packageName, emptySet())) {
              val appName = getAppName(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_PAUSED", appName, event.packageName, "App backgrounded")

              if (shouldAddLog("ACTIVITY_PAUSED", appName, event.packageName, currentTime)) {
                addLogEntry(logEntry)
              }
            }
          }
        }
      }
    } catch (e: SecurityException) {
    } catch (e: Exception) {
    }
  }

  private fun shouldAddLog(eventType: String, appName: String, packageName: String, currentTime: Long): Boolean {
    val minute = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(currentTime))
    val key = LogKey(appName, packageName, eventType, minute)

    // Check if we've seen this exact same log in the current minute
    val lastSeen = recentLogs[key]
    return if (lastSeen != null && (currentTime - lastSeen) < duplicateWindow) {
      false // Duplicate within the same minute
    } else {
      recentLogs[key] = currentTime
      true
    }
  }

  private fun shouldIgnoreOurApp(packageName: String): Boolean {
    val appsToIgnore = setOf(
      ourPackageName,
      ourPackageName2
    )
    return appsToIgnore.contains(packageName)
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
    if (packageName == ourPackageName) return false
    if (packageName == ourPackageName2) return false

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
    """.trimIndent()
  }

  private fun addLogEntry(logEntry: String) {
    cameraAccessLogs.add(logEntry)
    latestLogs.add(logEntry)

    if (cameraAccessLogs.size > 50) {
      cameraAccessLogs.poll()
    }
    if (latestLogs.size > 50) {
      latestLogs.poll()
    }

    saveLogToFile(logEntry)
  }

  private fun saveLogToFile(logEntry: String) {
    try {
      val directory = File(Environment.getExternalStorageDirectory(), "scuzero_logs")
      if (!directory.exists()) {
        directory.mkdirs()
      }
      val logFile = File(directory, "camera_logs.txt")
      val writer = FileWriter(logFile, true)
      writer.write("$logEntry\n\n")
      writer.write("=".repeat(50))
      writer.write("\n\n")
      writer.close()
    } catch (e: Exception) {
    }
  }

  private fun showCameraAlert(appName: String, packageName: String) {
    showBackgroundToast(appName)
    showAlertNotification(appName, packageName)
  }

  private fun showBackgroundToast(appName: String) {
    try {
      Toast.makeText(this, "📸 Camera accessed by: $appName", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
    }
  }

  private fun showAlertNotification(appName: String, packageName: String) {
    try {
      val alertId = System.currentTimeMillis().toInt()
      val notification = createAlertNotification(appName, packageName)
      notificationManager.notify(alertId, notification)
    } catch (e: Exception) {
    }
  }

  private fun getCurrentTime(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
  }

  fun getLogs(): List<String> {
    return cameraAccessLogs.toList().reversed()
  }

  override fun onDestroy() {
    isMonitoring = false
    recentLogs.clear()
    super.onDestroy()
  }
}
