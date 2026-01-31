# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Single test class
./gradlew test --tests "com.kingpaging.qwelcome.util.PhoneUtilsTest"

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lintDebug

# Clean build
./gradlew clean assembleDebug
```

**Note:** This project uses protobuf-gradle-plugin. Run `./gradlew clean` before major changes to `.proto` files.

## Architecture Overview

### MVVM with CompositionLocals

ViewModels are provided via `CompositionLocalProvider` in `MainActivity.kt`, **not** passed as parameters:

```kotlin
// Access in any composable:
val viewModel = LocalCustomerIntakeViewModel.current
val navigator = LocalNavigator.current
```

All providers defined in `di/CompositionLocals.kt`.

### Data Flow

1. **UI Layer** (`ui/`) — Stateless Compose screens
2. **ViewModel Layer** (`viewmodel/`) — StateFlow for UI state, SharedFlow for one-shot events
3. **Data Layer** (`data/`) — Proto DataStore persistence via `SettingsStore`
4. **Navigation** (`navigation/Navigator.kt`) — Abstraction for intents (SMS, Share, Clipboard)

### Key Classes

| Class | Purpose |
|-------|---------|
| `SettingsStore` | Proto DataStore wrapper for templates, tech profile, settings |
| `AppViewModelProvider` | Factory with process-wide singleton SettingsStore |
| `Template` | Message template with UUID-based ID for conflict detection |
| `Navigator` | Interface abstracting Android intents for testability |

## Critical Patterns

### Template System

- Built-in default has `id = "default"` (constant `DEFAULT_TEMPLATE_ID`)
- New templates use `UUID.randomUUID().toString()`
- UI sentinel for creation: `id = "__new__"`
- Required placeholders: `{{ customer_name }}`, `{{ ssid }}`

### Proto DataStore

Data stored in `user_preferences.pb`. Migration from legacy Preferences DataStore is automatic (checks `MIGRATION_COMPLETED_KEY`).

Proto definitions in `proto/src/main/proto/user_preferences.proto`.

### Phone Validation (NANP-Specific)

```kotlin
// Progressive mode for typing feedback, final mode for submit
validatePhoneNumber(phone, progressiveMode = true/false, resourceProvider)
```

Rules: 10 or 11 digits (11 must start with '1'), area code & exchange start with 2-9.

### SSID Validation

**32-byte limit** (UTF-8), not 32 characters. `WifiQrGenerator` validates bytes correctly.

### State Management

```kotlin
// UI state
private val _state = MutableStateFlow<UiState>()
val state: StateFlow<UiState> = _state.asStateFlow()

// One-shot events (Toasts)
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()
```

Use `collectAsStateWithLifecycle()` in Compose.

### Navigation State

Screen navigation uses enum with custom `Saver` (stable string keys) to survive enum renaming:

```kotlin
private val ScreenSaver = Saver<Screen, String>(
    save = { it.key },
    restore = { key -> Screen.entries.firstOrNull { it.key == key } ?: Screen.Main }
)
```

### Import/Export

Two formats with versioned schema (`EXPORT_SCHEMA_VERSION`):
- **TemplatePack** — Templates only (shareable)
- **FullBackup** — Templates + tech profile + settings

## Theme System

- Dual light/dark mode (follows system)
- Standard colors: `MaterialTheme.colorScheme.primary/secondary/tertiary`
- Custom colors: `LocalCyberColors.current` (success, warning, etc.)
- Dark mode check: `LocalDarkTheme.current`

Custom components: `NeonPanel`, `NeonButton`, `NeonTextField`, `CyberpunkBackdrop` in `ui/components/`.

## Testing

```kotlin
// Reset singleton in test setup
AppViewModelProvider.resetForTesting()

// Mock Navigator for tests
class FakeNavigator : Navigator { ... }

// Provide in tests via CompositionLocal
CompositionLocalProvider(LocalNavigator provides FakeNavigator()) { ... }
```

Existing tests cover `PhoneUtils`, `WifiQrGenerator`, `MessageTemplate`.

## Package Naming

- Use `import_pkg` (not `import` — reserved keyword)
- ViewModels: `viewmodel/feature/XxxViewModel.kt`
- Screens: `ui/feature/XxxScreen.kt`

## Text Input (Template Editor)

- Use `TextFieldState` / `rememberTextFieldState` / `BasicTextField` for multiline
- Never nest editors inside parent `verticalScroll`
- Apply `Modifier.imePadding()` for keyboard insets in dialogs
- Use `key(item.id)` when swapping/editing items

## Version Management

- Single source of truth: `version.properties` at project root (`VERSION_NAME`, `VERSION_CODE`)
- `app/build.gradle.kts` reads from this file — never hardcode versions in build files
- To release: add entries under `[Unreleased]` in `CHANGELOG.md`, then run `scripts/bump-version.sh <major|minor|patch>`
- The script updates the properties file, rewrites the changelog, commits, and creates a git tag
- Pushing a `v*` tag triggers the GitHub Actions release workflow which extracts the changelog into the release body

## SDK Requirements

- **minSdk:** 26 (Android 8.0)
- **targetSdk:** 36
- **Java:** 11
