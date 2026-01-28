# Q Welcome - Copilot Instructions

Android app for fiber technicians to send WiFi welcome messages. Kotlin + Jetpack Compose + Material3.

## Architecture Overview

**MVVM with CompositionLocals** - ViewModels are provided via `CompositionLocalProvider` in `MainActivity.kt`, not passed as parameters. Access them with `LocalCustomerIntakeViewModel.current`, `LocalSettingsViewModel.current`, etc.

```text
ui/                  # Compose screens (CustomerIntakeScreen, SettingsScreen, etc.)
viewmodel/           # ViewModels with StateFlow + SharedFlow for one-shot events
data/                # Models, SettingsStore (DataStore), ImportExportRepository
di/CompositionLocals.kt  # All LocalXxxViewModel providers
navigation/Navigator.kt  # Abstraction for intents (SMS, Share, Clipboard)
```

**Key Data Flow:**
- `SettingsStore` wraps DataStore for persistence (templates, tech profile, settings)
- `AppViewModelProvider` creates ViewModels with shared singleton `SettingsStore`
- One-shot events (Toasts) use `SharedFlow`, not state

## Critical Patterns

### Template System

Templates use UUID-based `id` for conflict detection. The built-in default has `id = "default"` (constant `DEFAULT_TEMPLATE_ID`). When creating new templates in UI, use sentinel `id = "__new__"` (see `TemplateListScreen.kt`).

Required placeholders: `{{ customer_name }}`, `{{ ssid }}` - validation in `Template.kt`.

### Theme: CyberpunkTheme

Use `MaterialTheme.colorScheme.primary/secondary/tertiary` for standard colors. For non-Material colors (success, warning), use `LocalCyberColors.current`. Check dark mode with `LocalDarkTheme.current`.

Custom components: `NeonPanel`, `NeonButton`, `NeonTextField`, `CyberpunkBackdrop` - all in `ui/components/`.

### Validation

Phone validation is NANP-specific with progressive feedback. See `CustomerIntakeViewModel.validatePhoneNumber()`. SSID validation should check **bytes** (UTF-8), not characters - max 32 bytes.

### Testing Considerations

- Use `Navigator` interface for testable intents (production: `AndroidNavigator`)
- Call `AppViewModelProvider.resetForTesting()` in test teardown
- CompositionLocals allow swapping ViewModels in tests

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease        # Release APK (needs signing config)
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests
```

## Import/Export Schema

JSON export uses versioned schema (`EXPORT_SCHEMA_VERSION` in `ExportModels.kt`). Two formats:
- **TemplatePack**: Just templates for sharing
- **FullBackup**: Templates + tech profile + settings

Imports validate schema version and detect conflicts by template UUID.

## Known Issues (from CODE_AUDIT.md)

- Error messages are hardcoded in `CustomerIntakeViewModel` companion object - should move to `strings.xml`
- SSID validation checks characters instead of bytes in ViewModel (QR generator does it correctly)
- Screen navigation uses enum with `rememberSaveable` - renaming enum values breaks saved state

## File Naming Conventions

- ViewModels: `XxxViewModel.kt` in `viewmodel/` or `viewmodel/feature/`
- Screens: `XxxScreen.kt` in `ui/` or `ui/feature/`
- Kotlin package for imports: `import_pkg` (not `import` - reserved keyword)

## Compose & Kotlin Requirements

**Dependencies:**
- Use AndroidX Compose BOM (`platform("androidx.compose:compose-bom:...")`), never hardcode individual Compose artifact versions
- Prefer stable APIs; isolate `@OptIn(ExperimentalXxx::class)` behind small wrappers when unavoidable

**State Management:**
- Use lifecycle-aware Flow collection: `collectAsStateWithLifecycle()` or `repeatOnLifecycle` patterns
- State hoisting: UI state lives in ViewModel, composables are stateless receivers
- Use `key(item.id)` when swapping/editing items so remembered state resets correctly

**Text Input (important for template editor):**
- For multiline editing, use state-based APIs: `TextFieldState` / `rememberTextFieldState` / `BasicTextField`
- Never nest text editors inside parent `verticalScroll` - let the editor own space via `Modifier.weight()` and handle its own scrolling
- Use `Dialog` (not `AlertDialog`) for complex/fullscreen editors; apply `Modifier.imePadding()` for keyboard insets

**Min/Target SDK:** minSdk 26+, targetSdk 36
