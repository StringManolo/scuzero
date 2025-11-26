let refreshInterval = null;

const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("scuzero: " + msg);
  } else {
    alert("System interface not available");
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
    statusElement.textContent = "Active";
    statusElement.className = "status active";
  } else {
    statusElement.textContent = "Inactive";
    statusElement.className = "status inactive";
  }
}

const refreshLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getCameraAccessLogs) {
    const logs = scuzero.getCameraAccessLogs();
    const logArea = document.getElementById('cameraLogs');
    logArea.value = logs;
    
    if (logs.trim() !== "") {
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

const startLogRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  refreshInterval = setInterval(refreshLogs, 1000);
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

window.addEventListener('load', function() {
  refreshLogs();
});

window.addEventListener('beforeunload', function() {
  stopLogRefresh();
});
