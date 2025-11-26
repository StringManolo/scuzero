const showNativeToast = (msg) => {
  if (typeof scuzero !== 'undefined' && scuzero.showToast) {
    scuzero.showToast("scuzero: " + msg || "No mensages to show");
  } else {
    alert("Update your SystemWebview");
  }
}

const getDeviceInfo = () => {
  if (typeof scuzero !== 'undefined' && scuzero.getDeviceInfo) {
    showNativeToast(scuzero.getDeviceInfo());
  }
}

