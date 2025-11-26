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
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class MicrophoneMonitorService : Service() {
  private val usageStatsManager by lazy {
    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
  }
  private val notificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val microphoneAccessLogs = ConcurrentLinkedQueue<String>()
  private var isMonitoring = false
  private val checkInterval = 1000L
  private val ourPackageName = "com.stringmanolo.scuzero"
  private val ourPackageName2 = "com.stringmanolo.scuzero.debug"

  private data class LogKey(val appName: String, val packageName: String, val eventType: String, val minute: String)
  private val recentLogs = LinkedHashMap<LogKey, Long>()
  private val duplicateWindow = 60000L

  companion object {
    const val NOTIFICATION_ID = 12348
    const val CHANNEL_ID = "microphone_monitor_channel"
    const val ALERT_CHANNEL_ID = "microphone_alert_channel"
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
        "Microphone Privacy Protection",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors microphone access continuously"
      }

      val alertChannel = NotificationChannel(
        ALERT_CHANNEL_ID,
        "Microphone Access Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts when microphone is accessed"
        enableVibration(true)
        enableLights(true)
      }

      notificationManager.createNotificationChannel(monitorChannel)
      notificationManager.createNotificationChannel(alertChannel)
    }
  }

  private fun createMonitoringNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Microphone Privacy Protection")
    .setContentText("Monitoring microphone access...")
    .setSmallIcon(android.R.drawable.ic_lock_lock)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .build()
  }

  private fun createAlertNotification(appName: String, packageName: String): Notification {
    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
    .setContentTitle("🎤 Microphone Access Detected")
    .setContentText("$appName is using the microphone")
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
      val previousForegroundApps = mutableSetOf<String>()

      while (isMonitoring) {
        try {
          val currentTime = System.currentTimeMillis()
          cleanOldRecentLogs(currentTime)

          val foregroundApp = getForegroundApp()
          if (foregroundApp != null && !previousForegroundApps.contains(foregroundApp)) {
            if (!shouldIgnoreApp(foregroundApp) && hasMicrophonePermission(foregroundApp)) {
              val appName = getAppName(foregroundApp)
              val detailedInfo = getAppDetailedInfo(foregroundApp)
              val logEntry = createDetailedLogEntry("FOREGROUND_APP", appName, foregroundApp, detailedInfo)

              if (shouldAddLog("FOREGROUND_APP", appName, foregroundApp, currentTime)) {
                addLogEntry(logEntry)
                showMicrophoneAlert(appName, foregroundApp)
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

  private fun checkUsageEvents(currentTime: Long) {
    try {
      val endTime = System.currentTimeMillis()
      val beginTime = endTime - checkInterval

      val events = usageStatsManager.queryEvents(beginTime, endTime)
      val event = UsageEvents.Event()

      while (events.hasNextEvent()) {
        events.getNextEvent(event)

        if (shouldIgnoreApp(event.packageName)) continue

        when (event.eventType) {
          UsageEvents.Event.ACTIVITY_RESUMED -> {
            if (hasMicrophonePermission(event.packageName)) {
              val appName = getAppName(event.packageName)
              val detailedInfo = getAppDetailedInfo(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_RESUMED", appName, event.packageName, detailedInfo)

              if (shouldAddLog("ACTIVITY_RESUMED", appName, event.packageName, currentTime)) {
                addLogEntry(logEntry)
                showMicrophoneAlert(appName, event.packageName)
              }
            }
          }
          UsageEvents.Event.ACTIVITY_PAUSED -> {
            if (hasMicrophonePermission(event.packageName)) {
              val appName = getAppName(event.packageName)
              val logEntry = createDetailedLogEntry("ACTIVITY_PAUSED", appName, event.packageName, "App backgrounded")

              if (shouldAddLog("ACTIVITY_PAUSED", appName, event.packageName, currentTime)) {
                addLogEntry(logEntry)
              }
            }
          }
        }
      }
    } catch (e: Exception) {
    }
  }

  private fun shouldAddLog(eventType: String, appName: String, packageName: String, currentTime: Long): Boolean {
    val minute = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(currentTime))
    val key = LogKey(appName, packageName, eventType, minute)

    val lastSeen = recentLogs[key]
    return if (lastSeen != null && (currentTime - lastSeen) < duplicateWindow) {
      false
    } else {
      recentLogs[key] = currentTime
      true
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

  private fun hasMicrophonePermission(packageName: String): Boolean {
    return try {
      val packageManager = applicationContext.packageManager
      val permissionCheck = packageManager.checkPermission(
        android.Manifest.permission.RECORD_AUDIO,
        packageName
      )
      permissionCheck == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
      false
    }
  }

  private fun shouldIgnoreApp(packageName: String?): Boolean {
    if (packageName == null) return false
    val appsToIgnore = setOf(ourPackageName, ourPackageName2)
    return appsToIgnore.contains(packageName)
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
        it.contains("RECORD_AUDIO") || it.contains("MICROPHONE") || it.contains("AUDIO")
      }?.joinToString(", ") ?: "No specific microphone permissions"

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
    🚨 MICROPHONE ACCESS DETECTED 🚨
    Event: $eventType
    Application: $appName
    Package: $packageName
    Timestamp: $timestamp

    Detailed Information:
    $details
    """.trimIndent()
  }

  private fun addLogEntry(logEntry: String) {
    microphoneAccessLogs.add(logEntry)
    latestLogs.add(logEntry)

    if (microphoneAccessLogs.size > 50) {
      microphoneAccessLogs.poll()
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
      val logFile = File(directory, "microphone_logs.txt")
      val writer = FileWriter(logFile, true)
      writer.write("$logEntry\n\n")
      writer.write("=".repeat(50))
      writer.write("\n\n")
      writer.close()
    } catch (e: Exception) {
    }
  }

  private fun showMicrophoneAlert(appName: String, packageName: String) {
    showBackgroundToast(appName)
    showAlertNotification(appName, packageName)
  }

  private fun showBackgroundToast(appName: String) {
    try {
      Toast.makeText(this, "🎤 Microphone accessed by: $appName", Toast.LENGTH_LONG).show()
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
    return microphoneAccessLogs.toList().reversed()
  }

  override fun onDestroy() {
    isMonitoring = false
    recentLogs.clear()
    super.onDestroy()
  }
}
