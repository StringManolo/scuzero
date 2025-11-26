package com.stringmanolo.scuzero

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class CameraMonitorService : Service() {
  private val usageStatsManager by lazy { 
    getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager 
  }
  private val notificationManager by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val cameraAccessLogs = ConcurrentLinkedQueue<String>()
  private var isMonitoring = false
  private val checkInterval = 2000L // 2 seconds

  companion object {
    const val NOTIFICATION_ID = 12346
    const val CHANNEL_ID = "camera_monitor_channel"
    var latestLogs = ConcurrentLinkedQueue<String>()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(NOTIFICATION_ID, createNotification())
    startMonitoring()
    return START_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Camera Access Monitor",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Monitors camera access and shows alerts"
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun createNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Camera Access Monitor")
    .setContentText("Monitoring camera usage...")
    .setSmallIcon(android.R.drawable.ic_dialog_alert)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    .setOngoing(true)
    .build()
  }

  private fun startMonitoring() {
    if (isMonitoring) return

    isMonitoring = true
    Thread {
      val knownCameraApps = setOf(
        "camera",
        "com.android.camera",
        "com.google.android.GoogleCamera",
        "com.sec.android.app.camera",
        "com.huawei.camera",
        "com.oneplus.camera"
      )

      val previousApps = mutableSetOf<String>()

      while (isMonitoring) {
        try {
          val endTime = System.currentTimeMillis()
          val beginTime = endTime - checkInterval

          val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
          )

          stats?.forEach { usageStats ->
            val packageName = usageStats.packageName
            val lastTimeUsed = usageStats.lastTimeUsed

            if (isCameraApp(packageName, knownCameraApps) && 
            !previousApps.contains(packageName) &&
            lastTimeUsed >= beginTime) {

              val appName = getAppName(packageName)
              val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
              val logEntry = "[$timestamp] $appName accessed camera"

              addLogEntry(logEntry)
              showAccessAlert(appName)
            }
          }

          previousApps.clear()
          stats?.forEach { previousApps.add(it.packageName) }

          Thread.sleep(checkInterval)
        } catch (e: InterruptedException) {
          break
        } catch (e: Exception) {
          continue
        }
      }
    }.start()
  }

  private fun isCameraApp(packageName: String, knownCameraApps: Set<String>): Boolean {
    return knownCameraApps.any { packageName.contains(it, ignoreCase = true) } ||
    packageName.contains("camera", ignoreCase = true)
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

  private fun addLogEntry(logEntry: String) {
    cameraAccessLogs.add(logEntry)
    latestLogs.add(logEntry)

    if (cameraAccessLogs.size > 50) {
      cameraAccessLogs.poll()
    }
    if (latestLogs.size > 50) {
      latestLogs.poll()
    }
  }

  private fun showAccessAlert(appName: String) {
    showToast("Camera accessed by: $appName")
    updateNotification("$appName using camera")
  }

  private fun showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  private fun updateNotification(message: String) {
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
    .setContentTitle("Camera Access Detected")
    .setContentText(message)
    .setSmallIcon(android.R.drawable.ic_dialog_alert)
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setOngoing(true)
    .build()
    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  fun getLogs(): List<String> {
    return cameraAccessLogs.toList().reversed()
  }

  override fun onDestroy() {
    isMonitoring = false
    super.onDestroy()
  }
}
