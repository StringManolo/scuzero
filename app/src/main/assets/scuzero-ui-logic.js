let refreshIntervals = {
  camera: null,
  microphone: null,
  gps: null,
  internet: null
};

let monitoringStatus = {
  camera: false,
  microphone: false,
  gps: false,
  internet: false
};

let lastLogContents = {
  camera: '',
  microphone: '',
  gps: '',
  internet: ''
};

// Theme Management
function initializeTheme() {
  const savedTheme = localStorage.getItem('scuzero-theme');
  const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
    enableDarkTheme();
  } else {
    enableLightTheme();
  }
}

function enableDarkTheme() {
  document.documentElement.setAttribute('data-theme', 'dark');
  document.getElementById('themeIcon').textContent = '☀️';
  localStorage.setItem('scuzero-theme', 'dark');
  
  // Update permission warning for dark theme
  const warning = document.getElementById('permissionWarning');
  if (warning.style.display !== 'none') {
    warning.classList.add('dark');
  }
}

function enableLightTheme() {
  document.documentElement.removeAttribute('data-theme');
  document.getElementById('themeIcon').textContent = '🌙';
  localStorage.setItem('scuzero-theme', 'light');
  
  // Update permission warning for light theme
  const warning = document.getElementById('permissionWarning');
  warning.classList.remove('dark');
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute('data-theme');
  if (currentTheme === 'dark') {
    enableLightTheme();
  } else {
    enableDarkTheme();
  }
}

// Tab Management
function openTab(tabName) {
  // Hide all tab contents
  const tabContents = document.querySelectorAll('.tab-content');
  tabContents.forEach(tab => {
    tab.style.display = 'none';
  });

  // Remove active class from all tab buttons
  const tabButtons = document.querySelectorAll('.tab-button');
  tabButtons.forEach(button => {
    button.classList.remove('active');
  });

  // Show selected tab and activate button
  document.getElementById(tabName).style.display = 'block';
  event.currentTarget.classList.add('active');
}

// Auto-scroll control
function shouldAutoScroll(logArea) {
  const threshold = 50; // pixels from bottom
  return logArea.scrollHeight - logArea.scrollTop - logArea.clientHeight <= threshold;
}

// Status Management
function updateMonitorStatus(monitorType, isActive) {
  const indicator = document.getElementById(monitorType + 'StatusIndicator');
  const statusText = document.getElementById(monitorType + 'StatusText');
  
  if (isActive) {
    indicator.classList.add('active');
    statusText.textContent = '🟢 Active - Real-time Monitoring';
    monitoringStatus[monitorType] = true;
  } else {
    indicator.classList.remove('active');
    statusText.textContent = '🔴 Inactive';
    monitoringStatus[monitorType] = false;
  }
}

// Common Functions
const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("🔒 " + msg);
  } else {
    // Fallback notification
    console.log("Toast: " + msg);
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
    permissionText.textContent = `Missing: ${missingPermissions.join(", ")}. These are required for monitoring.`;
    permissionWarning.style.display = 'block';
    
    // Apply theme class to warning
    if (document.documentElement.getAttribute('data-theme') === 'dark') {
      permissionWarning.classList.add('dark');
    }
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

// Enhanced Log Refresh Functions
const refreshCameraLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getCameraAccessLogs) {
    const logs = scuzero.getCameraAccessLogs();
    const logArea = document.getElementById('cameraLogs');
    const currentScroll = shouldAutoScroll(logArea);
    
    if (logs !== lastLogContents.camera) {
      logArea.value = logs || "No camera access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/camera_logs.txt";
      lastLogContents.camera = logs;
      
      // Only auto-scroll if user was near bottom
      if (currentScroll) {
        setTimeout(() => {
          logArea.scrollTop = logArea.scrollHeight;
        }, 100);
      }
    }
  }
}

const refreshMicrophoneLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getMicrophoneAccessLogs) {
    const logs = scuzero.getMicrophoneAccessLogs();
    const logArea = document.getElementById('microphoneLogs');
    const currentScroll = shouldAutoScroll(logArea);
    
    if (logs !== lastLogContents.microphone) {
      logArea.value = logs || "No microphone access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/microphone_logs.txt";
      lastLogContents.microphone = logs;
      
      if (currentScroll) {
        setTimeout(() => {
          logArea.scrollTop = logArea.scrollHeight;
        }, 100);
      }
    }
  }
}

const refreshGpsLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getGpsAccessLogs) {
    const logs = scuzero.getGpsAccessLogs();
    const logArea = document.getElementById('gpsLogs');
    const currentScroll = shouldAutoScroll(logArea);
    
    if (logs !== lastLogContents.gps) {
      logArea.value = logs || "No GPS access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/gps_logs.txt";
      lastLogContents.gps = logs;
      
      if (currentScroll) {
        setTimeout(() => {
          logArea.scrollTop = logArea.scrollHeight;
        }, 100);
      }
    }
  }
}

const refreshInternetLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getInternetAccessLogs) {
    const logs = scuzero.getInternetAccessLogs();
    const logArea = document.getElementById('internetLogs');
    const currentScroll = shouldAutoScroll(logArea);
    
    if (logs !== lastLogContents.internet) {
      logArea.value = logs || "No internet access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/internet_logs.txt";
      lastLogContents.internet = logs;
      
      if (currentScroll) {
        setTimeout(() => {
          logArea.scrollTop = logArea.scrollHeight;
        }, 100);
      }
    }
  }
}

// Monitor Control Functions (Camera)
const startCameraMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.startCameraMonitoring();
    showNativeToast("Camera Monitoring: " + result);
    
    if (result === "camera_monitoring_started") {
      updateMonitorStatus('camera', true);
      startLogRefresh('camera');
      checkPermissions();
    } else if (result === "usage_permission_required") {
      showNativeToast("Usage Access permission required!");
      checkPermissions();
    }
  } catch (error) {
    showNativeToast("Error starting camera monitor: " + error);
  }
}

const stopCameraMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.stopCameraMonitoring();
    showNativeToast("Camera Monitoring: " + result);
    
    if (result === "camera_monitoring_stopped") {
      updateMonitorStatus('camera', false);
      stopLogRefresh('camera');
    }
  } catch (error) {
    showNativeToast("Error stopping camera monitor: " + error);
  }
}

const clearCameraLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.clearCameraLogs) {
    const result = scuzero.clearCameraLogs();
    showNativeToast("Camera Logs: " + result);
    refreshCameraLogs();
  }
}

const copyCameraLogs = () => {
  const logArea = document.getElementById('cameraLogs');
  const logs = logArea.value;
  
  if (logs && logs.trim() !== "" && logs !== "No camera access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/camera_logs.txt") {
    if (typeof scuzero !== 'undefined' && scuzero.copyToClipboard) {
      scuzero.copyToClipboard(logs);
    } else {
      logArea.select();
      document.execCommand('copy');
      showNativeToast("Camera logs copied to clipboard");
    }
  } else {
    showNativeToast("No camera logs to copy");
  }
}

// Microphone Monitor Functions
const startMicrophoneMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.startMicrophoneMonitoring();
    showNativeToast("Microphone Monitoring: " + result);
    
    if (result === "microphone_monitoring_started") {
      updateMonitorStatus('microphone', true);
      startLogRefresh('microphone');
      checkPermissions();
    } else if (result === "usage_permission_required") {
      showNativeToast("Usage Access permission required!");
      checkPermissions();
    }
  } catch (error) {
    showNativeToast("Error starting microphone monitor: " + error);
  }
}

const stopMicrophoneMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.stopMicrophoneMonitoring();
    showNativeToast("Microphone Monitoring: " + result);
    
    if (result === "microphone_monitoring_stopped") {
      updateMonitorStatus('microphone', false);
      stopLogRefresh('microphone');
    }
  } catch (error) {
    showNativeToast("Error stopping microphone monitor: " + error);
  }
}

const clearMicrophoneLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.clearMicrophoneLogs) {
    const result = scuzero.clearMicrophoneLogs();
    showNativeToast("Microphone Logs: " + result);
    refreshMicrophoneLogs();
  }
}

const copyMicrophoneLogs = () => {
  const logArea = document.getElementById('microphoneLogs');
  const logs = logArea.value;
  
  if (logs && logs.trim() !== "" && logs !== "No microphone access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/microphone_logs.txt") {
    if (typeof scuzero !== 'undefined' && scuzero.copyToClipboard) {
      scuzero.copyToClipboard(logs);
    } else {
      logArea.select();
      document.execCommand('copy');
      showNativeToast("Microphone logs copied to clipboard");
    }
  } else {
    showNativeToast("No microphone logs to copy");
  }
}

// GPS Monitor Functions
const startGpsMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.startGpsMonitoring();
    showNativeToast("GPS Monitoring: " + result);
    
    if (result === "gps_monitoring_started") {
      updateMonitorStatus('gps', true);
      startLogRefresh('gps');
      checkPermissions();
    } else if (result === "usage_permission_required") {
      showNativeToast("Usage Access permission required!");
      checkPermissions();
    }
  } catch (error) {
    showNativeToast("Error starting GPS monitor: " + error);
  }
}

const stopGpsMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.stopGpsMonitoring();
    showNativeToast("GPS Monitoring: " + result);
    
    if (result === "gps_monitoring_stopped") {
      updateMonitorStatus('gps', false);
      stopLogRefresh('gps');
    }
  } catch (error) {
    showNativeToast("Error stopping GPS monitor: " + error);
  }
}

const clearGpsLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.clearGpsLogs) {
    const result = scuzero.clearGpsLogs();
    showNativeToast("GPS Logs: " + result);
    refreshGpsLogs();
  }
}

const copyGpsLogs = () => {
  const logArea = document.getElementById('gpsLogs');
  const logs = logArea.value;
  
  if (logs && logs.trim() !== "" && logs !== "No GPS access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/gps_logs.txt") {
    if (typeof scuzero !== 'undefined' && scuzero.copyToClipboard) {
      scuzero.copyToClipboard(logs);
    } else {
      logArea.select();
      document.execCommand('copy');
      showNativeToast("GPS logs copied to clipboard");
    }
  } else {
    showNativeToast("No GPS logs to copy");
  }
}

// Internet Monitor Functions
const startInternetMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.startInternetMonitoring();
    showNativeToast("Internet Monitoring: " + result);
    
    if (result === "internet_monitoring_started") {
      updateMonitorStatus('internet', true);
      startLogRefresh('internet');
      checkPermissions();
    } else if (result === "usage_permission_required") {
      showNativeToast("Usage Access permission required!");
      checkPermissions();
    }
  } catch (error) {
    showNativeToast("Error starting internet monitor: " + error);
  }
}

const stopInternetMonitoring = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    return;
  }

  try {
    const result = scuzero.stopInternetMonitoring();
    showNativeToast("Internet Monitoring: " + result);
    
    if (result === "internet_monitoring_stopped") {
      updateMonitorStatus('internet', false);
      stopLogRefresh('internet');
    }
  } catch (error) {
    showNativeToast("Error stopping internet monitor: " + error);
  }
}

const clearInternetLogs = () => {
  if (typeof scuzero !== 'undefined' && scuzero.clearInternetLogs) {
    const result = scuzero.clearInternetLogs();
    showNativeToast("Internet Logs: " + result);
    refreshInternetLogs();
  }
}

const copyInternetLogs = () => {
  const logArea = document.getElementById('internetLogs');
  const logs = logArea.value;
  
  if (logs && logs.trim() !== "" && logs !== "No internet access events detected yet...\n\nLogs are automatically saved to ./scuzero_logs/internet_logs.txt") {
    if (typeof scuzero !== 'undefined' && scuzero.copyToClipboard) {
      scuzero.copyToClipboard(logs);
    } else {
      logArea.select();
      document.execCommand('copy');
      showNativeToast("Internet logs copied to clipboard");
    }
  } else {
    showNativeToast("No internet logs to copy");
  }
}

// Common Functions
const startLogRefresh = (monitorType) => {
  if (refreshIntervals[monitorType]) {
    clearInterval(refreshIntervals[monitorType]);
  }
  refreshIntervals[monitorType] = setInterval(() => {
    switch (monitorType) {
      case 'camera': refreshCameraLogs(); break;
      case 'microphone': refreshMicrophoneLogs(); break;
      case 'gps': refreshGpsLogs(); break;
      case 'internet': refreshInternetLogs(); break;
    }
  }, 1500);
}

const stopLogRefresh = (monitorType) => {
  if (refreshIntervals[monitorType]) {
    clearInterval(refreshIntervals[monitorType]);
    refreshIntervals[monitorType] = null;
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

// Initialize on load
window.addEventListener('load', function() {
  // Initialize theme
  initializeTheme();
  
  // Add theme toggle listener
  document.getElementById('themeToggle').addEventListener('click', toggleTheme);
  
  // Initialize logs
  refreshCameraLogs();
  refreshMicrophoneLogs();
  refreshGpsLogs();
  refreshInternetLogs();
  
  // Check permissions
  checkPermissions();
  
  // Listen for system theme changes
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
    if (!localStorage.getItem('scuzero-theme')) {
      if (e.matches) {
        enableDarkTheme();
      } else {
        enableLightTheme();
      }
    }
  });
});

window.addEventListener('beforeunload', function() {
  // Stop all refresh intervals
  Object.keys(refreshIntervals).forEach(monitorType => {
    stopLogRefresh(monitorType);
  });
});

// Auto-refresh logs for active monitors every 5 seconds
setInterval(() => {
  Object.keys(monitoringStatus).forEach(monitorType => {
    if (monitoringStatus[monitorType]) {
      switch (monitorType) {
        case 'camera': refreshCameraLogs(); break;
        case 'microphone': refreshMicrophoneLogs(); break;
        case 'gps': refreshGpsLogs(); break;
        case 'internet': refreshInternetLogs(); break;
      }
    }
  });
}, 5000);
