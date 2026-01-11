# 🚦 Federated Urban Insights (FUI)

> **Privacy-First Traffic & Crowd Analysis using Kotlin Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Web-green.svg)]()

## 📖 Overview

Federated Urban Insights (FUI) is a **serverless, privacy-first** application that analyzes traffic density and crowd levels from images. All processing happens **entirely on-device** - no data is ever sent to external servers.

### Key Features

- 🚗 **Vehicle Detection**: Counts vehicles using advanced color-blob detection algorithms
- 👥 **Crowd Estimation**: Estimates pedestrian density through edge detection and skin-tone analysis
- 🎯 **Scene Recognition**: Automatically detects scene type (traffic, indoor, nature, etc.)
- 🔒 **100% Privacy**: All analysis runs locally on your device
- 📱 **Cross-Platform**: Works on Android and Web browsers

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| Android | Native Android SDK |
| Web | Vaadin Flow Framework |
| Shared Logic | Kotlin Multiplatform |
| Build System | Gradle with Kotlin DSL |

## 🚀 Getting Started

### Prerequisites

- **JDK 17+** - [Download](https://adoptium.net/)
- **Android Studio** (for Android development) - [Download](https://developer.android.com/studio)
- **Gradle 8.0+** (or use included wrapper)

### Running the Android App

1. Clone the repository:
```bash
git clone https://github.com/yunuse1/Fui.git
cd Fui
```

2. Open in Android Studio:
   - File → Open → Select the `fui` folder
   - Wait for Gradle sync to complete

3. Run on device/emulator:
   - Select `androidApp` configuration
   - Click Run (▶️) or press `Shift+F10`

4. Usage:
   - Tap **"Gallery"** to select an image from your device
   - Tap **"Camera"** to take a photo
   - Tap **"Demo"** to generate a test traffic image
   - Results appear automatically after image selection

### Running the Web App

1. Navigate to project root:
```bash
cd fui
```

2. Run the web application:
```bash
./gradlew :webApp:bootRun
```

3. Open in browser:
```
http://localhost:8080
```

4. Usage:
   - Drag & drop or click to upload an image
   - View real-time analysis results
   - Use "Clear" to reset

### Building for Production

```bash
# Build all modules
./gradlew build

# Build Android APK
./gradlew :androidApp:assembleDebug

# Build Web JAR
./gradlew :webApp:bootJar
```

## 📊 Analysis Algorithm

The application uses sophisticated computer vision algorithms implemented entirely in Kotlin:

### Vehicle Detection
1. **Road Region Identification**: Analyzes color patterns to identify road areas
2. **Color-Blob Analysis**: Detects vehicle-like color patterns (white, black, colored vehicles)
3. **Morphological Filtering**: Removes noise and validates vehicle shapes
4. **Flood-Fill Counting**: Groups connected pixels into vehicle blobs

### Crowd Estimation
1. **Skin-Tone Detection**: Identifies human presence through RGB color analysis
2. **Edge Density Calculation**: Measures visual complexity indicating crowd density
3. **Scene-Adaptive Scaling**: Adjusts estimates based on detected scene type

### Scene Classification
- Sky ratio analysis for outdoor/indoor detection
- Road surface pattern recognition
- Color variety measurement
- Horizontal line detection for traffic infrastructure

## 📁 Project Structure

```
fui/
├── shared/                 # Kotlin Multiplatform shared code
│   └── src/
│       ├── commonMain/     # Common Kotlin code for all platforms
│       ├── androidMain/    # Android-specific implementations
│       └── nativeMain/     # Native platform implementations
├── androidApp/             # Android application
│   └── src/main/
│       ├── java/           # Kotlin source files
│       └── res/            # Android resources (layouts, strings)
├── webApp/                 # Web application (Vaadin)
│   └── src/main/
│       └── kotlin/         # Vaadin views and services
├── build.gradle.kts        # Root build configuration
└── settings.gradle.kts     # Module settings
```

## 🔒 Privacy & Data Handling

FUI is designed with **privacy as a core principle**:

- ✅ **No Network Requests**: All image processing is done locally
- ✅ **No Data Storage**: Images are processed in memory and discarded
- ✅ **No Tracking**: No analytics or telemetry
- ✅ **Open Source**: Full code transparency

This makes FUI suitable for:
- Privacy-conscious users
- Areas with limited connectivity
- Offline deployments
- GDPR-compliant applications

## 📸 Screenshots

### Android App
- Upload images from gallery or camera
- Real-time traffic and crowd analysis
- Scene type detection
- Detailed analysis reports

### Web App
- Modern dark theme UI
- Drag-and-drop image upload
- Live statistics dashboard
- Responsive design

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 📝 Contest Essay

Read the full story behind this project in the [ESSAY.md](ESSAY.md) file.

## 🙏 Acknowledgments

- Built with ❤️ using Kotlin
- Submitted for KotlinConf 2026 Student Coding Competition
- Inspired by the need for privacy-preserving urban analytics

---

**Note**: This is a demonstration project showcasing Kotlin Multiplatform capabilities. The analysis algorithms are heuristic-based and designed for educational purposes.
