# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`web_print` is a Flutter plugin (v0.0.4) that enables printing web content (HTML/URLs) to Bluetooth thermal printers. The Android implementation is fully functional; the iOS implementation is a stub.

- **Package:** com.akuple.web_print
- **Dart SDK:** ^3.7.2, **Flutter:** >=3.3.0
- **Android:** minSdk 18, compileSdk 35, Kotlin 2.1.0, Java 17
- **iOS:** Platform 8.0+, Swift 5.0 (incomplete implementation)

## Build & Test Commands

```bash
# Get dependencies
flutter pub get

# Run tests
flutter test

# Run a single test file
flutter test test/web_print_test.dart

# Analyze code
flutter analyze

# Run the example app
cd example && flutter run

# Build Android
cd example && flutter build apk
```

## Architecture

### Communication Flow

Flutter (Dart) communicates with native platforms via a single **MethodChannel** named `web_print`:

```
Dart (WebPrint static methods)
  → MethodChannel('web_print')
    → Android: WebPrintPlugin.kt (MethodCallHandler)
    → iOS: SwiftWebPrintPlugin.swift (stub)
```

### Dart Layer (`lib/`)

Two files:
- `web_print.dart` — Static API: `printWebView()`, `getBluetoothPairedDevices()`, `openBluetoothSetting()`, `testPlugin()`
- `models/PrinterBluetoothDevice.dart` — Data model with JSON serialization for Bluetooth device info (name, address)

### Android Native (`android/`)

Kotlin + Java hybrid:

- **WebPrintPlugin.kt** — Entry point. Implements `FlutterPlugin`, `MethodCallHandler`, `ActivityAware`. Routes method channel calls.
- **WebPrinterService.kt** — Core print logic. Converts HTML/URL → PDF (via WebView print adapter) → bitmap (via PDFBox) → ESC/POS commands sent to thermal printer. Targets 80mm paper width.
- **WebPrintUtils.kt** — Bluetooth device enumeration, WebView setup, Bluetooth settings launcher.
- **AsyncBluetoothEscPosPrint.java** / **AsyncEscPosPrint.java** — Background AsyncTask classes for non-blocking Bluetooth ESC/POS printing with error status codes.

Key dependency: [DantSu/ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android) v3.4.0 (via Jitpack), plus PDFBox Android 2.0.27.0 for PDF rendering.

### iOS Native (`ios/`)

`SwiftWebPrintPlugin.swift` is a minimal stub using `UIPrintInteractionController` with a hardcoded URL. Needs full implementation to match Android capabilities.

## Key Technical Details

- The Android printing pipeline is: HTML/URL → WebView → PrintAdapter → PDF → PDFBox bitmap rendering → trim whitespace → scale to 80mm width → ESC/POS byte commands → Bluetooth socket
- Bluetooth permissions are required: classic BT permissions plus Android 12+ runtime permissions (BLUETOOTH_CONNECT, BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE)
- The `printWebView` method accepts parameters: `address` (printer MAC), `url` (web URL or HTML string), and `topOffset` (vertical offset)
- All Dart plugin methods are static and use `MethodChannel.invokeMethod`
