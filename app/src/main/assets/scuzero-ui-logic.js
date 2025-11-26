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

const toggleCamera = async () => {
  const checkbox = document.getElementById('cameraToggle');
  const statusElement = document.getElementById('cameraStatus');
  
  if (typeof scuzero === 'undefined') {
    showNativeToast("Interface not available");
    return;
  }

  try {
    const isEnabled = checkbox.checked;
    
    if (isEnabled) {
      // Enable camera
      const result = scuzero.enableCamera();
      if (result === "camera_enabled") {
        statusElement.textContent = "Enabled";
        statusElement.className = "status enabled";
        showNativeToast("Camera enabled");
      } else {
        checkbox.checked = false;
        statusElement.textContent = "Error";
        statusElement.className = "status admin-required";
      }
    } else {
      const result = scuzero.disableCamera();
      if (result === "camera_disabled") {
        statusElement.textContent = "Disabled";
        statusElement.className = "status disabled";
        showNativeToast("Camera disabled");
      } else if (result === "admin_required") {
        statusElement.textContent = "Permissions required";
        statusElement.className = "status admin-required";
        showNativeToast("Activate administrator permissions");
      } else {
        checkbox.checked = true;
        statusElement.textContent = "Error";
        statusElement.className = "status admin-required";
      }
    }
  } catch (error) {
    showNativeToast("Error: " + error);
    checkbox.checked = !checkbox.checked;
  }
}

const checkCameraStatus = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("Interface not available");
    return;
  }

  const status = scuzero.getCameraStatus();
  const checkbox = document.getElementById('cameraToggle');
  const statusElement = document.getElementById('cameraStatus');

  switch (status) {
    case "admin_required":
      statusElement.textContent = "Permissions required";
      statusElement.className = "status admin-required";
      checkbox.checked = true;
      checkbox.disabled = false;
      showNativeToast("Administrator permissions required");
      break;
    case "enabled":
      statusElement.textContent = "Enabled";
      statusElement.className = "status enabled";
      checkbox.checked = true;
      checkbox.disabled = false;
      showNativeToast("Camera enabled");
      break;
    case "disabled":
      statusElement.textContent = "Disabled";
      statusElement.className = "status disabled";
      checkbox.checked = false;
      checkbox.disabled = false;
      showNativeToast("Camera disabled");
      break;
    default:
      statusElement.textContent = "Unknown status";
      statusElement.className = "status admin-required";
      checkbox.disabled = true;
  }
}

const checkAdminStatus = () => {
  if (typeof scuzero === 'undefined') {
    showNativeToast("Interface not available");
    return;
  }

  const isAdmin = scuzero.isAdminActive();
  if (isAdmin) {
    showNativeToast("✓ Administrator permissions active");
  } else {
    showNativeToast("✗ Administrator permissions required");
  }
}

window.addEventListener('load', function() {
  setTimeout(checkCameraStatus, 1000);
});
