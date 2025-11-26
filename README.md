# 🛡️ Scuzero - Privacy Hardening Android App

**Monitor and protect your privacy on Android** - Scuzero detects and logs when applications access your sensors and personal data without your knowledge.

## ✨ Key Features

### 🔍 Real-time Monitoring
- **📷 Camera**: Detects when apps access your camera
- **🎤 Microphone**: Monitors background microphone usage  
- **📍 GPS**: Tracks location access attempts
- **🌐 Internet**: Controls application network connections

### 🎯 Modern Interface
- **Responsive design** that adapts to any device
- **Dark/light mode** with smooth transitions
- **Intuitive tab navigation**
- **Real-time logs** with terminal formatting
- **Visual indicators** for monitoring status

### 📊 Log Management
- Automatic logging to `.txt` files
- Copy and clear log functionality
- Organized storage in `./scuzero_logs/`
- Timestamps and detailed access information

## 🚀 Installation

### Prerequisites
- Android 8.0+ (API 26+)

### Build from Source

```bash
# Clone the project
git clone https://github.com/yourusername/scuzero.git
cd scuzero

# Generate keystore (first time)
bash/bash/utils-generate-keystore

# Compile APK
bash/bash/utils-compile-apk
```

### Permission Setup
1. Install the APK
2. Grant the permissions
3. Enable usage stats access

## 🏗️ Project Architecture

```
scuzero/
├── app/                          # Main application module
│   ├── src/main/java/com/stringmanolo/scuzero/
│   │   ├── MainActivity.kt              # Main activity
│   │   ├── CameraMonitorService.kt      # Camera monitoring service
│   │   ├── MicrophoneMonitorService.kt  # Microphone monitoring service
│   │   ├── GpsMonitorService.kt         # GPS monitoring service
│   │   └── InternetMonitorService.kt    # Internet monitoring service
│   ├── assets/
│   │   ├── index.html            # Main web interface
│   │   └── scuzero-ui-logic.js   # UI JavaScript logic
│   └── res/                      # Android resources
├── bash/                         # Utility scripts
│   ├── utils-generate-keystore   # Keystore generation
│   └── utils-compile-apk         # Automatic compilation
└── fastlane/                     # Publishing configuration
    └── metadata/                 # Google Play metadata
```

## 🛠️ Technologies Used

- **Kotlin** - Primary language for Android logic
- **WebView** - HTML/CSS/JS interface rendering
- **Gradle** - Build system and dependencies
- **Android Services** - Background monitoring
- **Material Design** - Modern design system
- **Github Actions** - CI/CD

## 📱 Usage

1. **Start Monitoring**: Press "Start" on any tab to begin
2. **View Logs**: Access attempts show in real-time in log area
3. **Manage Permissions**: Use Settings section to configure permissions
4. **Export Data**: Copy logs or access saved files

## 🔒 Required Permissions

| Permission | Purpose |
|------------|---------|
| `Device Admin` | System access monitoring (not yet) |
| `Usage Stats` | App usage detection |
| `Camera` | Camera access monitoring |
| `Microphone` | Microphone access monitoring |
| `Location` | GPS access monitoring |

## 🎨 Customization

The web interface is highly customizable through CSS variables:

```css
:root {
  --primary-color: #2563eb;
  --background: #ffffff;
  --text-primary: #1e293b;
  /* Customize these values to change the theme */
}
```

## 🤝 Contributing

Contributions are welcome. Please:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

If you encounter any issues or have questions:

- Open an [issue](https://github.com/stringmanolo/scuzero/issues)

---

**Scuzero** - Take control of your Android privacy 🛡️
