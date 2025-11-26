const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("scuzero: " + msg);
  } else {
    alert("System interface not available");
  }
}

const getDeviceInfo = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getDeviceInfo) {
    showNativeToast(scuzero.getDeviceInfo());
  }
}

const toggleCameraProtection = () => {
  const checkbox = document.getElementById('cameraToggle');
  const statusElement = document.getElementById('cameraStatus');
  
  if (typeof scuzero === 'undefined') {
    showNativeToast("System interface not available");
    checkbox.checked = false;
    return;
  }

  if (checkbox.checked) {
    enableCameraProtection();
  } else {
    disableCameraProtection();
  }
}

const enableCameraProtection = () => {
  const statusElement = document.getElementById('cameraStatus');
  
  try {
    statusElement.textContent = "Activating...";
    statusElement.className = "status";
    
    const result = scuzero.enableAdvancedCameraBlock();
    showNativeToast("Protection: " + result);
    
    if (result.includes("block_status")) {
      statusElement.textContent = "Active";
      statusElement.className = "status disabled";
    } else if (result.includes("failed")) {
      statusElement.textContent = "Failed";
      statusElement.className = "status error";
      document.getElementById('cameraToggle').checked = false;
    } else {
      statusElement.textContent = "Unknown";
      statusElement.className = "status error";
    }
  } catch (error) {
    showNativeToast("Error enabling protection");
    statusElement.textContent = "Error";
    statusElement.className = "status error";
    document.getElementById('cameraToggle').checked = false;
  }
}

const disableCameraProtection = () => {
  const statusElement = document.getElementById('cameraStatus');
  
  try {
    statusElement.textContent = "Deactivating...";
    statusElement.className = "status";
    
    const result = scuzero.disableAdvancedCameraBlock();
    showNativeToast("Protection: " + result);
    
    if (result.includes("disabled")) {
      statusElement.textContent = "Inactive";
      statusElement.className = "status enabled";
    } else if (result.includes("failed")) {
      statusElement.textContent = "Error";
      statusElement.className = "status error";
      document.getElementById('cameraToggle').checked = true;
    } else {
      statusElement.textContent = "Unknown";
      statusElement.className = "status error";
    }
  } catch (error) {
    showNativeToast("Error disabling protection");
    statusElement.textContent = "Error";
    statusElement.className = "status error";
    document.getElementById('cameraToggle').checked = true;
  }
}

const checkProtectionStatus = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getCameraBlockStatus) {
    const status = scuzero.getCameraBlockStatus();
    console.log("Camera protection status:", status);
  }
}

window.addEventListener('load', function() {
  setTimeout(() => {
    checkProtectionStatus();
  }, 1000);
});
