# Q Welcome — Build & Design Audit

## PHASE PLAN & STATUS

Use this section to track progress. Each phase should land in its own PR unless noted.

### Phase 1 — Architecture refactor ✅ [PR #21](https://github.com/H2OKing89/QWelcome/pull/21) merged

- [x] SoundManager lifecycle wiring (finding #7) — already fixed in app
- [x] Decouple UI from SoundManager (finding #8)

### Phase 2 — Accessibility + string resources 🔄 [PR #22](https://github.com/H2OKing89/QWelcome/pull/22) in review

- [x] Fix missing content descriptions (finding #12)
- [x] Replace hardcoded UI strings with string resources (finding #13)

### Phase 3 — Code health 🔄 [PR #23](https://github.com/H2OKing89/QWelcome/pull/23) in review

- [x] Split large composables (finding #17)
- [x] Split ImportExportRepository (finding #18)
- [x] Split SettingsStore mappers/migration (finding #19)
- [x] Centralize validation rules (finding #20)
- [x] Use data object for UiEvent objects (finding #21)

### Phase 4 — Testing 🔄 [PR #24](https://github.com/H2OKing89/QWelcome/pull/24) in review

- [x] Add Compose UI tests (finding #22)
- [x] Add missing ViewModel tests (finding #23)
- [x] Add DataStore migration test (finding #24)
- [x] Remove ExampleUnitTest (finding #25)

### Optional — Build/Gradle polish

- [ ] Set Kotlin JVM target explicitly (finding #3)
- [ ] Remember a single AppViewModelProvider instance (finding #4)
- [ ] Enable Gradle parallel builds (finding #5)
- [ ] Remove unused View Material dep if safe (finding #6)

### Release guidance

- Phase 1 is refactoring only — not worth a standalone release.
- Prefer bundling Phases 2-4 for a release with user-visible impact.
- If you need a quick patch, ship after Phase 2 (accessibility + strings).

## BUILD & GRADLE

### Positives

- **Version catalog** (libs.versions.toml) is well-structured with clear section grouping
- **Compose BOM** correctly used — individual Compose artifact versions aren't hardcoded
- **Proto DataStore** properly split into a `:proto` module with `java-library` plugin
- **R8/ProGuard** config is thorough — serialization, protobuf, Crashlytics, and coroutine rules all present
- Signing config cleanly falls back between CI env vars and local keystore
- version.properties as single source of truth for versioning is a great pattern

### Issues & Recommendations

| # | Severity | Finding | Detail |
|---|----------|---------|--------|
| 3 | **Low** | **Missing `kotlin.jvm.target`** | Java 11 is set in `compileOptions` but the Kotlin JVM target isn't explicitly set in the Gradle files. While `kotlin-compose` plugin may handle it, explicit `jvmToolchain(11)` or `kotlinOptions { jvmTarget = "11" }` prevents mismatches. |
| 4 | **Low** | **`AppViewModelProvider` creates a new factory per `viewModel()` call** | In MainActivity.kt, five `AppViewModelProvider(applicationContext)` instances are created. Each resolves to the same internal singletons via `Companion`, so it's functionally correct — but wasteful. Consider `remember`-ing a single factory instance. |
| 5 | **Low** | **Parallel mode disabled** | gradle.properties has `org.gradle.parallel` commented out. Enabling it reduces multi-module build times. |
| 6 | **Info** | **`google-material` alongside Material3** | You pull in both `com.google.android.material:material` and `androidx.compose.material3:material3`. If you only use Compose M3, the View-based Material dependency may be dead weight (unless used by splash theme XML). |

---

## ARCHITECTURE

### Positives

- **Clean MVVM layers**: `data/` → `viewmodel/` → `ui/` separation is consistent
- **CompositionLocal DI** is well-documented in CompositionLocals.kt with fail-fast error messages
- **`Navigator` abstraction** (Navigator.kt) properly decouples Android intents from ViewModels — excellent for testability
- **`ResourceProvider`** interface (ResourceProvider.kt) keeps ViewModels free of `Context`
- **`TimeProvider`** (TimeProvider.kt) uses monotonic clock and is mockable — thoughtful design
- **Type-safe navigation routes** (Routes.kt) using `@Serializable` objects
- **One-shot events** via `SharedFlow` is the correct modern pattern (no `LiveData` misuse)
- Proto DataStore migration from Preferences is carefully implemented with diagnostics

### Issues & Recommendations

| # | Severity | Finding | Detail |
|---|----------|---------|--------|
| 7 | **Medium** | **`SoundManager` is a global singleton with unmanaged lifecycle** | SoundManager.kt creates its own `CoroutineScope` but `shutdown()` is never called anywhere in the app. The docstring says to wire it to `ProcessLifecycleOwner` — but this hasn't been done. Audio resources could leak. |
| 8 | **Medium** | **`SoundManager` called directly from UI** | In CustomerIntakeScreen.kt and SettingsScreen.kt, `SoundManager.playBeep()` is called directly from composables. This couples the UI layer to a concrete singleton and makes testing harder. Consider emitting sound events from the ViewModel or injecting `SoundManager`. |
| 9 | **Medium** | **Manual singleton DI** | `AppViewModelProvider.Companion` maintains `@Volatile` singletons with double-checked locking (AppViewModelProvider.kt). This works but is fragile — consider Hilt or at minimum a simple `AppContainer` in your `Application` class to centralize wiring. |
| 10 | **Low** | **`Navigator` shows Toasts** | Navigator.kt catches exceptions and shows `Toast` directly. This mixes navigation/intent abstraction with UI feedback. Consider letting exceptions propagate (or returning a `Result`) so the ViewModel can emit the appropriate `UiEvent`. |
| 11 | **Low** | **`UpdateChecker` uses raw `HttpURLConnection`** | UpdateChecker.kt manually manages HTTP connections. For a single API call this is acceptable, but OkHttp or Ktor would give you better timeout handling, connection pooling, and maintainability. |

---

## UI / DESIGN

### Positives

- **Cyberpunk theme is carefully crafted** — full dark + light palettes in CyberpunkTheme.kt with proper Material3 color token mapping
- **Extended colors** (`CyberExtendedColors` for success/warning/lime) done correctly via `CompositionLocal`
- **Deprecated `CyberScheme`** properly marked with `@Deprecated` + `ReplaceWith`
- **Typography** uses custom fonts (Orbitron/Exo2) with appropriate text shadow tiering for dark/light
- **Button hierarchy** (Primary SMS → Secondary Share → Tertiary Copy) follows Material3 emphasis guidelines
- **`rememberSaveable`** used correctly for rotation-surviving state vs `remember` for transient state
- **Accessibility semantics** present: `semantics { heading() }`, `Role.Checkbox`, `mergeDescendants`
- **`imePadding()`** applied to the scrollable form — keyboard handling is correct
- **Edge-to-edge** enabled with transparent scaffold containers

### Issues & Recommendations

| # | Severity | Finding | Detail |
|---|----------|---------|--------|
| 12 | **Medium** | **10 instances of `contentDescription = null`** | Icons in SettingsScreen.kt (4 icons), ImportScreen.kt, ExportScreen.kt, QrCodeBottomSheet.kt, TemplateListScreen.kt, and NeonComponents.kt have null content descriptions. These are invisible to screen readers. For decorative icons, wrap in a `Box` with `Modifier.semantics { }` instead. For functional icons, add meaningful descriptions. |
| 13 | **Medium** | **Hardcoded strings in Compose** | Several UI strings are hardcoded: `"Customer Name"`, `"WiFi SSID"`, `"WiFi Password"`, `"Send"`, `"SMS"`, `"Share"`, `"Copy"`, `"Template"`, `"Settings"`, `"Discard"`, `"Keep editing"` etc. in CustomerIntakeScreen.kt and SettingsScreen.kt. Use `stringResource(R.string.*)` consistently for potential localization and lint compliance. |
| 14 | **Low** | **`@file:OptIn(ExperimentalMaterial3Api::class)` is file-wide** | Both CustomerIntakeScreen.kt and SettingsScreen.kt opt into the entire experimental API at file level. Prefer scoped `@OptIn` on specific composables to catch future breaking changes. |
| 15 | **Low** | **Wildcard imports** | SettingsScreen.kt uses `import androidx.compose.material3.*` and `import androidx.compose.runtime.*`. Explicit imports improve readability and prevent accidental symbol collisions. |
| 16 | **Low** | **`Color.Transparent` as Scaffold `containerColor`** | Used in both main screens. This is intentional for the backdrop effect but bypasses Material3's elevation-toning system. Document this explicitly as a design decision so it's not mistakenly "fixed" later. |

---

## CODE QUALITY

| # | Severity | Finding | Detail |
|---|----------|---------|--------|
| 17 | **Medium** | **`CustomerIntakeScreen` is 489 lines** | This single composable does template selection, form input, validation display, QR sheet, action buttons, and event collection. Extract sub-composables: `TemplateSelector`, `CustomerFormFields`, `ActionButtonRow`, `QrCodeSection`. |
| 18 | **Medium** | **`ImportExportRepository` is 562 lines** | ImportExportRepository.kt handles export, import, validation, conflict detection, and full backup — all in one class. Consider splitting into `ExportService` + `ImportService` + `BackupService`. |
| 19 | **Medium** | **`SettingsStore` is 336 lines** | SettingsStore.kt contains the DataStore, migration logic, all CRUD operations, AND proto mappers. Move mappers to a separate `Mappers.kt` and the migration to a `PreferencesToProtoMigration.kt`. |
| 20 | **Low** | **Duplicate validation logic** | SSID byte-length and password length validation are implemented in both `CustomerIntakeViewModel` ([line 168-176](app/src/main/java/com/kingpaging/qwelcome/viewmodel/CustomerIntakeViewModel.kt#L168)) and `WifiQrGenerator` (WifiQrGenerator.kt). Single source of truth for validation rules would prevent drift. |
| 21 | **Low** | **`UiEvent` sealed class uses `object` without `data object`** | In CustomerIntakeViewModel.kt: `CopySuccess`, `ValidationFailed`, `ActionFailed`, `RateLimitExceeded` use `object` instead of Kotlin 1.9+ `data object`. Not breaking, but `data object` gives you proper `toString()` for debugging. |

---

## TESTING

### Positives

- **Good unit test infrastructure**: `FakeNavigator`, `FakeResourceProvider`, `FakeTimeProvider`, `MainDispatcherRule` in testutil/
- **Turbine** for Flow testing — correct modern library choice
- **MockK** for mocking — appropriate for Kotlin
- **`AppViewModelProvider.resetForTesting()`** prevents state leakage between tests

### Gaps

| # | Severity | Finding |
|---|----------|---------|
| 22 | **Medium** | **No UI/Compose tests** — `androidTest/` appears empty. No `ComposeTestRule` tests for screen behavior, navigation, or accessibility assertions. |
| 23 | **Medium** | **No test for `SettingsViewModel`, `ExportViewModel`, `ImportViewModel`, `TemplateListViewModel`** — only `CustomerIntakeViewModel` has unit tests. The export/import and settings ViewModels have test directories but coverage should be verified. |
| 24 | **Low** | **No integration test for Proto DataStore migration** — the migration from Preferences → Proto is complex (including the corrupted data diagnostics). This is a prime candidate for a Robolectric or instrumented test. |
| 25 | **Low** | **`ExampleUnitTest.kt` still exists** — The default placeholder test file should be removed. |

---

## SECURITY

| # | Severity | Finding |
|---|----------|---------|
| 26 | **Info** | **Auto-clear after 10 min background** is a nice security feature for technician workflows — prevents stale customer data. |
| 27 | **Info** | **Rate limiting** on send/share/copy actions prevents accidental SMS spam — thoughtful. |
| 28 | **Low** | **`UpdateChecker` TRUSTED_UPDATE_HOSTS** whitelist is correct but the actual download URL validation logic should be reviewed — ensure the check is applied before opening any URI. |
