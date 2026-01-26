# Code Audit Report: Q Welcome

**Date:** January 25, 2026
**Reviewer:** Claude Code
**Branch:** feature/theme-refactor

---

## Overall Assessment: Solid for a First App

For a first mobile app, this is well-structured. You've followed many modern Android best practices. The issues identified below are refinements rather than fundamental problems.

---

## What You Did Well

1. **Clean MVVM Architecture** - Good separation between UI, ViewModel, and data layers
2. **Jetpack Compose** - Modern UI toolkit, good choice
3. **StateFlow/SharedFlow** - Proper reactive state management
4. **Navigator Abstraction** - Makes testing possible by abstracting Android intents
5. **DataStore** - Modern persistence over deprecated SharedPreferences
6. **Input Validation** - Progressive real-time feedback is excellent UX
7. **Auto-clear Security** - 10-minute timeout clears sensitive data (needs lifecycle fix)
8. **Rate Limiting** - Prevents accidental double-taps
9. **Import/Export System** - Well-designed with conflict detection and schema versioning
10. **Testability** - Factory pattern and interfaces enable unit testing

---

## Critical Issues

### 1. Package Name Still Uses `com.example`

**File:** `app/build.gradle.kts:9`

```kotlin
namespace = "com.kingpaging.qwelcome"
applicationId = "com.kingpaging.qwelcome"
```

**Problem:** `com.example` is reserved for samples. Google Play will reject this.

**Fix:** Change to your company domain (e.g., `com.kingpaging.qwelcome`).

**Impact:** Cannot publish to Play Store without fixing this.

---

## Medium Issues

### 2. Screen State Not Type-Safe

**File:** `MainActivity.kt:88`

```kotlin
var currentScreen by rememberSaveable { mutableStateOf(Screen.Main) }
```

**Problem:** `rememberSaveable` stores enum by name. If you rename enum values, saved state will crash on restore after app update.

**Recommendation:** Consider using Navigation Compose library for type-safe navigation, or implement a custom `Saver` for the enum.

---

### 3. SSID Validation Bug - Bytes vs Characters

**File:** `CustomerIntakeViewModel.kt:242-244`

```kotlin
} else if (currentState.ssid.length > 32) {
    _uiState.update { it.copy(ssidError = ERROR_SSID_TOO_LONG) }
```

**Problem:** SSID limit is 32 **bytes** (UTF-8), not 32 characters. A single emoji can be 4 bytes. Your `WifiQrGenerator` correctly checks bytes, but the ViewModel checks characters.

**Fix:** Use byte length check:

```kotlin
} else if (currentState.ssid.toByteArray(Charsets.UTF_8).size > 32) {
```

---

### 4. Hardcoded Error Messages

**File:** `CustomerIntakeViewModel.kt:39-49`

```kotlin
// TODO: Move these to strings.xml for proper localization support
const val ERROR_NAME_EMPTY = "Customer name is required"
```

**Problem:** Error messages are hardcoded in Kotlin. This prevents localization/translation.

**Fix:** Move all user-facing strings to `res/values/strings.xml`.

---

### 5. Missing Lifecycle Connection for Auto-Clear

**File:** `CustomerIntakeViewModel.kt:105-117`

The ViewModel has `onPause()` and `onResume()` methods for the auto-clear feature, but they are never called from the Activity.

**Fix:** Add lifecycle observer in `MainActivity.kt`:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> customerIntakeViewModel.onPause()
            Lifecycle.Event.ON_RESUME -> customerIntakeViewModel.onResume()
            else -> {}
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

---

### 6. No Back Button/Gesture Handling

**File:** `MainActivity.kt:92-129`

There's no handling for the system back button/gesture. Users on non-Main screens cannot use the back gesture to navigate.

**Fix:** Add `BackHandler` from Compose:

```kotlin
import androidx.activity.compose.BackHandler

// Inside setContent, after screen state declarations:
BackHandler(enabled = currentScreen != Screen.Main) {
    currentScreen = when (currentScreen) {
        Screen.TemplateList -> templateListOrigin
        Screen.Export, Screen.Import -> Screen.Settings
        else -> Screen.Main
    }
}
```

---

### 7. Deprecated Storage Permissions

**File:** `AndroidManifest.xml:6-9`

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**Problem:** With `minSdk = 26`, you may not need these permissions at all if you're only saving to app-private directories or using MediaStore/FileProvider.

**Recommendation:** Review if these are actually needed. The FileProvider you have is the modern approach for sharing files.

---

## Minor Issues / Suggestions

### 8. Multiple StateFlow Updates Per Validation

**File:** `CustomerIntakeViewModel.kt:218-267`

```kotlin
_uiState.update { it.copy(customerNameError = ERROR_NAME_EMPTY) }
hasError = true
// ... later
_uiState.update { it.copy(customerPhoneError = ERROR_PHONE_EMPTY) }
```

**Problem:** Multiple `_uiState.update` calls cause multiple recompositions.

**Suggestion:** Batch all error updates into a single state change:

```kotlin
_uiState.update { state ->
    state.copy(
        customerNameError = if (state.customerName.isBlank()) ERROR_NAME_EMPTY else null,
        customerPhoneError = if (requirePhone && state.customerPhone.isBlank()) ERROR_PHONE_EMPTY else null,
        ssidError = if (state.ssid.isBlank()) ERROR_SSID_EMPTY else null,
        // ... all validations at once
    )
}
```

---

### 9. JSON Parsing on Main Thread

**File:** `ImportExportRepository.kt:115-128`

JSON parsing in `validateImport()` happens on whatever thread calls it. For large imports, this could cause UI jank.

**Suggestion:** Ensure callers use `Dispatchers.IO`:

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    val result = repository.validateImport(jsonString)
    // switch to Main for UI updates
}
```

---

### 10. ProGuard Rules for Serialization

Verify that `proguard-rules.pro` includes proper keep rules for kotlinx.serialization. Without them, release builds will crash due to reflection/minification issues.

**Required rules:**

```proguard
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.kingpaging.qwelcome.**$$serializer { *; }
-keepclassmembers class com.kingpaging.qwelcome.** {
    *** Companion;
}
-keepclasseswithmembers class com.kingpaging.qwelcome.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

---

### 11. Template Storage Scalability

**File:** `SettingsStore.kt:99-110`

Templates are stored as a single JSON string in DataStore. Every save operation re-serializes and writes the entire blob.

**Current impact:** Fine for typical use (< 50 templates).

**Future consideration:** If templates grow significantly, consider migrating to Room database with proper indices.

---

## Project Structure Assessment

| Aspect | Status | Notes |
| -------- | -------- | ------- |
| Single activity architecture | Good | Modern approach |
| Package organization | Good | Clear separation (data, ui, viewmodel, util, navigation, di) |
| Version catalog | Good | Using `libs.versions.toml` |
| UI state separation | Good | `CustomerIntakeUiState` data class |
| Navigation | Consider | Manual enum works but Navigation Compose is more robust |
| Dependency injection | Consider | Manual factory works but Hilt/Koin reduces boilerplate |

---

## Security Assessment

| Area | Status | Notes |
| ------ | -------- | ------- |
| Data storage | Good | Local-only, DataStore |
| Auto-clear sensitive data | Needs Fix | Not connected to lifecycle |
| No hardcoded secrets | Good | No API keys or credentials |
| FileProvider for sharing | Good | Secure file access |
| Input validation | Good | Comprehensive validation |
| SQL injection | N/A | No database |
| Tech profile privacy | Good | Excluded from template pack exports |
| Intent safety | Good | Uses FLAG_ACTIVITY_NEW_TASK correctly |

---

## Priority Summary

| Priority | Issue | Effort | Impact |
| -------- | ------- | -------- | -------- |
| Critical | Change `com.example` package name | Medium | Blocks Play Store |
| High | Connect lifecycle to ViewModel pause/resume | Low | Security feature broken |
| High | Add BackHandler for navigation | Low | UX issue |
| Medium | Fix SSID byte vs character validation | Low | Edge case bug |
| Medium | Add ProGuard serialization rules | Low | Release build crash |
| Low | Move error strings to resources | Medium | Localization |
| Low | Batch StateFlow updates | Low | Performance |

---

## Recommended Next Steps

1. **Before Release:**
   - Change package name from `com.example`
   - Add lifecycle observer for auto-clear feature
   - Add BackHandler for system back gesture
   - Verify ProGuard rules in a release build

2. **Quality Improvements:**
   - Fix SSID byte length validation
   - Batch StateFlow updates in validateInputs()
   - Move hardcoded strings to resources

3. **Future Considerations:**
   - Migrate to Navigation Compose for type-safe navigation
   - Consider Hilt for dependency injection
   - Add unit tests for ViewModels and utilities

---

## Files Reviewed

- `MainActivity.kt`
- `CustomerIntakeViewModel.kt`
- `SettingsStore.kt`
- `ImportExportRepository.kt`
- `Template.kt`
- `AppViewModelProvider.kt`
- `Navigator.kt`
- `PhoneUtils.kt`
- `WifiQrGenerator.kt`
- `AndroidManifest.xml`
- `build.gradle.kts`
