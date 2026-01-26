# Q Welcome

A modern Android app for fiber internet technicians to quickly send professional WiFi welcome messages to new customers.

![Android](https://img.shields.io/badge/Android-26%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Features

- 📱 **Quick Message Generation** — Fill in customer details and instantly generate a welcome message
- 📨 **Multiple Send Options** — SMS, Share sheet, or Copy to clipboard
- 📶 **QR Code Generation** — Generate WiFi QR codes customers can scan to connect
- 🎨 **Cyberpunk Theme** — Beautiful dark/light mode with neon accents
- 📝 **Custom Templates** — Create and manage multiple message templates
- 💾 **Import/Export** — Backup and share templates between devices
- 🔒 **Privacy-Focused** — All data stays on device, auto-clears after 10 minutes of inactivity
- ✅ **Input Validation** — Real-time validation with helpful error messages

## Screenshots

<!-- Add screenshots here -->
<!-- ![Main Screen](docs/screenshots/main.png) -->

## Requirements

- Android 8.0 (API 26) or higher
- ~10 MB storage

## Installation

### Option A: Download APK

1. Go to [Releases](https://github.com/H2OKing89/QWelcome/releases)
2. Download the latest `app-release.apk`
3. Enable "Install from unknown sources" if prompted
4. Install and enjoy!

### Option B: Build from Source

```bash
# Clone the repo
git clone https://github.com/H2OKing89/QWelcome.git
cd QWelcome

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with StateFlow
- **Storage:** DataStore (Preferences)
- **QR Generation:** QRose library
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36

## Project Structure

```text
app/src/main/java/com/kingpaging/qwelcome/
├── data/           # Data models, repository, DataStore
├── di/             # Dependency injection (CompositionLocal providers)
├── navigation/     # Navigator abstraction for intents
├── ui/             # Compose screens and components
│   ├── components/ # Reusable UI components
│   ├── theme/      # Colors, typography, theming
│   ├── settings/   # Settings screen
│   ├── templates/  # Template management
│   ├── export/     # Export functionality
│   └── import_pkg/ # Import functionality
├── util/           # Utility classes (QR, phone formatting, etc.)
└── viewmodel/      # ViewModels for each screen
```

## Development

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 11+
- Android SDK 36

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Code Style

This project follows standard Kotlin coding conventions. The codebase uses:
- MVVM architecture
- Unidirectional data flow with StateFlow
- Compose for all UI

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [QRose](https://github.com/nicholasodnotnieto/qrose) for QR code generation
- [Orbitron Font](https://fonts.google.com/specimen/Orbitron) for the cyberpunk display font
- Jetpack Compose team for the amazing UI toolkit
