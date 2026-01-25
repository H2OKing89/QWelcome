# Customer Intake SMS App — Android Development Plan

**Platform:** Native Android (Kotlin + Jetpack Compose)  
**Purpose:** Personal tool for sending WiFi welcome messages to new customers  
**Distribution:** Sideloaded APK (not Play Store)

---

## v1 MVP Features (Complete)

- ✅ **Input Form Screen:** All fields implemented.
- ✅ **State Management:** Uses a `ViewModel` and `UiState` class to manage form data.
- ✅ **Input Validation:** All fields are required and validated, with error messages displayed inline.
- ✅ **Message Template:** Generates a welcome message from a template stored in `strings.xml`.
- ✅ **SMS Intent:** Opens the user's default messaging app with the phone number and message pre-filled.
- ✅ **Share Intent:** Includes a "Share" button as a fallback for apps like Google Voice.
- ✅ **Copy to Clipboard:** A "Copy" button provides a universal fallback.
- ✅ **Phone Number Normalization:** Logic correctly formats US phone numbers.
- ✅ **Deduplication:** Stores a timestamp when a message is sent (dialog prompt is on hold).
- ✅ **Password Visibility Toggle:** The password field has an icon to show/hide the text.
- ✅ **Polished Layout:** The app uses a `Scaffold` layout with a `TopAppBar`.
- ✅ **Dark Mode:** Automatically adapts to the system light/dark theme.

---

## v2 Quality-of-Life Features (Backlog)

| Feature | Description |
|---------|-------------|
| **Tech Profile / Signature Settings** | Store tech name/title/department locally and append to message. |
| **Saved Templates** | Multiple message templates (different regions, services) stored locally. |
| **Recent Customers** | List of last 50 sends with tap-to-resend. |
| **Contact Picker** | Pull phone number from Android Contacts. |
| **QR/Barcode Scan** | Auto-fill account number from equipment label. |
| **Export History** | CSV export of sent messages (for records). |

---

## Build & Install

### Generate APK

1. **Android Studio:** Build → Build Bundle(s) / APK(s) → Build APK(s)
2. APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Sideload to Phone

**Option A: USB**

```bash
adb install app-debug.apk
```

**Option B: File Transfer**

1. Copy APK to phone (Google Drive, email, USB)
2. Open APK on phone and allow installation from unknown sources.
