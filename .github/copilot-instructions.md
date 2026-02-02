# Q Welcome - Copilot Instructions

Android app for fiber technicians to send WiFi welcome messages. Kotlin + Jetpack Compose + Material3.

## Big Picture
- MVVM + CompositionLocals: ViewModels are provided in `MainActivity.kt` via `CompositionLocalProvider`, not passed as parameters. Access with `LocalXxxViewModel.current` from `di/CompositionLocals.kt`.
- Layers: `ui/` (stateless Compose screens) → `viewmodel/` (StateFlow state + SharedFlow events) → `data/` (DataStore, repositories).
- Navigation uses type-safe routes in `navigation/Routes.kt` and graph in `navigation/AppNavGraph.kt`. Intent actions are abstracted behind `navigation/Navigator.kt` for testability.
- Persistence is Proto DataStore via `data/SettingsStore` and `proto/src/main/proto/user_preferences.proto` (migration handled internally).

## Project-Specific Patterns
- Templates: UUID-based `id` for conflict detection; built-in default uses `DEFAULT_TEMPLATE_ID = "default"`. UI creation sentinel is `"__new__"` (see `ui/templates/TemplateListScreen.kt`). Required placeholders: `{{ customer_name }}`, `{{ ssid }}` validated in `data/Template.kt`.
- One-shot UI events (toasts/snackbars) use `SharedFlow`, not state. Collect with `collectAsStateWithLifecycle()`.
- Phone validation is NANP-specific with progressive feedback: `CustomerIntakeViewModel.validatePhoneNumber()`.
- SSID validation is byte-based (UTF-8, max 32 bytes) used in `util/WifiQrGenerator`.
- Theme: standard colors from `MaterialTheme.colorScheme`; non-Material colors via `LocalCyberColors.current` and dark mode via `LocalDarkTheme.current`. Custom components live in `ui/components/`.
- Text input (template editor): use `TextFieldState` / `rememberTextFieldState` / `BasicTextField`; never nest editors in a parent `verticalScroll`; use `Dialog` + `Modifier.imePadding()`.

## Workflows
- Build: `./gradlew assembleDebug`, `./gradlew assembleRelease` (signing needed), `./gradlew test`, `./gradlew connectedAndroidTest`, `./gradlew lintDebug`.
- Proto changes: run `./gradlew clean` before major `.proto` edits.
- Versioning: single source of truth in `version.properties`; release with `scripts/bump-version.sh <major|minor|patch>` which updates `CHANGELOG.md`, commits, and tags.
- Git hooks: `scripts/git-hooks/pre-commit` blocks commits directly to `master`/`main` unless `--no-verify`.

## Conventions
- Package naming: use `import_pkg` (not `import`).
- Files: ViewModels in `viewmodel/feature/*ViewModel.kt`, screens in `ui/feature/*Screen.kt`, routes in `navigation/Routes.kt` (serializable objects/data classes).
- Dependencies: use the Compose BOM in Gradle; avoid hardcoding Compose artifact versions.
