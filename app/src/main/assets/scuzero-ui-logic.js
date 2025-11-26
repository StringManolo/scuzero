const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("scuzero: " + msg || "No messages to show");
  } else {
    alert("Update your SystemWebview");
  }
}

const getDeviceInfo = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getDeviceInfo) {
    showNativeToast(scuzero.getDeviceInfo());
  }
}

const enableCameraBlock = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("Interface not available");
    return;
  }

  try {
    const result = scuzero.enableAdvancedCameraBlock();
    showNativeToast("Block result: " + result);
    
    if (result.includes("block_status")) {
      updateUI("blocked");
    } else if (result.includes("failed")) {
      updateUI("error");
    }
  } catch (error) {
    showNativeToast("Error: " + error);
  }
}

const disableCameraBlock = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("Interface not available");
    return;
  }

  try {
    const result = scuzero.disableAdvancedCameraBlock();
    showNativeToast("Unblock result: " + result);
    
    if (result.includes("disabled")) {
      updateUI("unblocked");
    } else if (result.includes("failed")) {
      updateUI("error");
    }
  } catch (error) {
    showNativeToast("Error: " + error);
  }
}

const updateUI = (status) => {
  const statusElement = document.getElementById('cameraStatus');
  const toggleButton = document.getElementById('cameraToggle');
  
  if (!statusElement || !toggleButton) return;
  
  switch (status) {
    case "blocked":
      statusElement.textContent = "Camera Blocked";
      statusElement.className = "status disabled";
      toggleButton.checked = false;
      break;
    case "unblocked":
      statusElement.textContent = "Camera Unblocked";
      statusElement.className = "status enabled";
      toggleButton.checked = true;
      break;
    case "error":
      statusElement.textContent = "Error Occurred";
      statusElement.className = "status admin-required";
      break;
  }
}

const toggleCamera = () => {
  const checkbox = document.getElementById('cameraToggle');
  if (checkbox.checked) {
    disableCameraBlock();
  } else {
    enableCameraBlock();
  }
}

window.addEventListener('load', function() {
  setTimeout(() => {
    if (typeof scuzero !== 'undefined' && scuzero.getCameraBlockStatus) {
      const status = scuzero.getCameraBlockStatus();
        showNativeToast(`Camera block status: ${status}`);
    }
  }, 1000);
});
