let refreshInterval = null;
let isMonitoring = false;

const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("🔒 " + msg);
  } else {
    alert("System interface not available");
  }
}

const checkPermissions = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  const permissionWarning = document.getElementById('permissionWarning');
  const permissionText = document.getElementById('permissionText');
  const missingPermissions = [];

  if (!scuzero.hasUsageStatsPermission()) {
    missingPermissions.push("Usage Access");
  }

  if (missingPermissions.length > 0) {
    permissionText.textContent = `Missing: ${missingPermissions.join(", ")}. These are required for camera monitoring.`;
    permissionWarning.style.display = 'block';
  } else {
    permissionWarning.style.display = 'none';
    showNativeToast("All permissions granted ✓");
  }
}

const fixPermissions = () => {
  if (typeof scuzero !== 'undefined' && scuzero.openUsageAccessSettings) {
    scuzero.openUsageAccessSettings();
  } else {
    showNativeToast("Cannot open settings");
  }
}

const startMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.startCameraMonitoring();
    showNativeToast("Monitoring: " + result);
    
    if (result === "monitoring_started") {
      updateMonitorStatus(true);
      startLogRefresh();
      checkPermissions();
    } else if (result === "usage_permission_required") {
      showNativeToast("Usage Access permission required!");
      checkPermissions();
    }
  } catch (error) {
    showNativeToast("Error starting monitor: " + error);
  }
}

const stopMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.stopCameraMonitoring();
    showNativeToast("Monitoring: " + result);
    
    if (result === "monitoring_stopped") {
      updateMonitorStatus(false);
      stopLogRefresh();
    }
  } catch (error) {
    showNativeToast("Error stopping monitor: " + error);
  }
}

const updateMonitorStatus = (isActive) => {
  const statusElement = document.getElementById('monitorStatus');
  if (isActive) {
    statusElement.textContent = "🟢 Active - Real-time Monitoring";
    statusElement.className = "status active";
    isMonitoring = true;
  } else {
    statusElement.textContent = "🔴 Inactive";
    statusElement.className = "status inactive";
    isMonitoring = false;
  }
}

const refreshLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getCameraAccessLogs) {
    const logs = scuzero.getCameraAccessLogs();
    const logArea = document.getElementById('cameraLogs');
    logArea.value = logs || "No camera access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/camera_logs.txt";
    
    if (logs && logs.trim() !== "" && logs !== "No camera access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/camera_logs.txt") {
      logArea.scrollTop = logArea.scrollHeight;
    }
  }
}

const clearLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.clearCameraLogs) {
    const result = scuzero.clearCameraLogs();
    showNativeToast("Logs: " + result);
    refreshLogs();
  }
}

const copyLogs = () => {
  const logArea = document.getElementById('cameraLogs');
  const logs = logArea.value;
  
  if (logs && logs.trim() !== "" && logs !== "No camera access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/camera_logs.txt") {
    if (typeof scuzero !== 'undefined' && scuzero.copyToClipboard) {
      scuzero.copyToClipboard(logs);
    } else {
      logArea.select();
      document.execCommand('copy');
      showNativeToast("Logs copied to clipboard");
    }
  } else {
    showNativeToast("No logs to copy");
  }
}

const startLogRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  refreshInterval = setInterval(refreshLogs, 1500);
}

const stopLogRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
}

const openUsageSettings = () => {
  if (typeof scuzero !== 'undefined' && scuzero.openUsageAccessSettings) {
    scuzero.openUsageAccessSettings();
  } else {
    showNativeToast("Function not available");
  }
}

const requestCameraPermission = () => {
  if (typeof scuzero !== 'undefined' && scuzero.requestCameraPermission) {
    const result = scuzero.requestCameraPermission();
    showNativeToast("Permission: " + result);
  } else {
    showNativeToast("Function not available");
  }
}

window.addEventListener('load', function() {
  refreshLogs();
  checkPermissions();
  
  setTimeout(() => {
    if (typeof scuzero !== 'undefined' && scuzero.getDeviceInfo) {
      const deviceInfo = scuzero.getDeviceInfo();
      console.log("Device Info:", deviceInfo);
    }
  }, 1000);
});

window.addEventListener('beforeunload', function() {
  stopLogRefresh();
});

setInterval(() => {
  if (isMonitoring) {
    refreshLogs();
  }
}, 5000);
