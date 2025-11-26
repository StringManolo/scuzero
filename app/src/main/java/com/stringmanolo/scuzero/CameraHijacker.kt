package com.stringmanolo.scuzero

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdvancedCameraBlocker(private val context: Context) {
  private val camera2Hijacker = Camera2Hijacker(context)
  private val legacyHijacker = CameraHijacker()

  fun enableCompleteCameraBlock(): String {
    return try {
      val serviceIntent = Intent(context, CameraLockService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }

      val camera2Result = camera2Hijacker.hijackWithCamera2()
      val legacyResult = legacyHijacker.hijackAllCameras()

      "block_status:camera2=$camera2Result,legacy=$legacyResult"
    } catch (e: Exception) {
      "complete_block_failed:${e.message}"
    }
  }

  fun disableCameraBlock(): String {
    return try {
      camera2Hijacker.releaseAll()
      legacyHijacker.releaseAll()

      val serviceIntent = Intent(context, CameraLockService::class.java)
      context.stopService(serviceIntent)

      "camera_block_disabled"
    } catch (e: Exception) {
      "disable_failed:${e.message}"
    }
  }
}

class Camera2Hijacker(private val context: Context) {
  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private val cameraSessions = mutableMapOf<String, CameraCaptureSession>()
  private val cameraDevices = mutableMapOf<String, CameraDevice>()

  fun hijackWithCamera2(): String {
    return try {
      val cameraIds = cameraManager.cameraIdList
      var hijackedCount = 0

      cameraIds.forEach { cameraId ->
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
              override fun onOpened(camera: CameraDevice) {
                cameraDevices[cameraId] = camera
                try {
                  camera.createCaptureSession(emptyList(), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                      cameraSessions[cameraId] = session
                      hijackedCount++
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                      hijackedCount++
                    }
                  }, null)
                } catch (e: CameraAccessException) {
                  hijackedCount++
                }
              }
              override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevices.remove(cameraId)
              }
              override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevices.remove(cameraId)
              }
            }, null)
          }
        } catch (e: CameraAccessException) {
        } catch (e: SecurityException) {
        }
      }

      "hijack_attempted:$hijackedCount/${cameraIds.size}"
    } catch (e: Exception) {
      "camera2_hijack_failed:${e.message}"
    }
  }

  fun releaseAll() {
    cameraSessions.values.forEach { session ->
      try {
        session.close()
      } catch (e: Exception) {
      }
    }
    cameraDevices.values.forEach { device ->
      try {
        device.close()
      } catch (e: Exception) {
      }
    }
    cameraSessions.clear()
    cameraDevices.clear()
  }
}

class CameraHijacker {
  private val cameras = mutableListOf<Camera>()

  fun hijackAllCameras(): String {
    return try {
      var hijackedCount = 0
      try {
        val cameraCount = Camera.getNumberOfCameras()
        for (i in 0 until cameraCount) {
          try {
            val camera = Camera.open(i)
            camera.setPreviewCallback { _, _ -> }
            cameras.add(camera)
            hijackedCount++
          } catch (e: Exception) {
          }
        }
      } catch (e: Exception) {
      }

      if (cameras.isEmpty()) {
        try {
          val camera = Camera.open()
          camera.setPreviewCallback { _, _ -> }
          cameras.add(camera)
          hijackedCount++
        } catch (e: Exception) {
        }
      }

      "hijacked:$hijackedCount"
    } catch (e: Exception) {
      "legacy_hijack_failed:${e.message}"
    }
  }

  fun releaseAll() {
    cameras.forEach { camera ->
      try {
        camera.release()
      } catch (e: Exception) {
      }
    }
    cameras.clear()
  }
}

class CameraLockService : Service() {
  private val notificationId = 12345
  private val channelId = "camera_lock_channel"

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground(notificationId, createNotification())
    return START_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Camera Privacy Protection",
        NotificationManager.IMPORTANCE_LOW
      )
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun createNotification(): Notification {
    return NotificationCompat.Builder(this, channelId)
    .setContentTitle("Camera Privacy Protection")
    .setContentText("Camera access is being monitored")
    .setSmallIcon(android.R.drawable.ic_lock_lock)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .build()
  }
}
