# Changelog

All notable changes to Q Welcome will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

No unreleased changes.

## [2.4.0] - 2026-02-08

### Added

- **String Resources for Localization** - Migrated all hardcoded UI strings to `strings.xml` across CustomerIntakeScreen, SettingsScreen, NeonComponents, QrCodeBottomSheet, ExportScreen, ImportScreen, and TemplateListScreen with comprehensive accessibility support including content descriptions for screen readers
- **NeonDropdownMenuBox Component** - New cyberpunk-styled dropdown wrapper component that encapsulates Material3 ExposedDropdownMenuBox with experimental API annotations, providing reusable dropdown functionality with consistent theming
- **NeonTopAppBar Component** - New cyberpunk-styled top app bar wrapper component that encapsulates Material3 TopAppBar with experimental API annotations, ensuring consistent navigation bar styling across screens
- **SoundManager Lifecycle Management** - App-level lifecycle observer in QWelcomeApplication for automatic SoundManager restart on app foreground and shutdown on background with comprehensive error handling and Crashlytics logging
- **Lifecycle Process Dependency** - Added androidx.lifecycle:lifecycle-process for ProcessLifecycleOwner support in app lifecycle management

### Changed

- **WiFi Validation Refactoring** - Centralized SSID and password validation logic in WifiQrGenerator utility, removing duplicate validation code from CustomerIntakeViewModel and improving maintainability
- **Experimental API Isolation** - Removed `@OptIn(ExperimentalMaterial3Api::class)` annotations from screen composables (CustomerIntakeScreen, SettingsScreen) by encapsulating experimental APIs in dedicated wrapper components
- **AppViewModelFactory Optimization** - Use `remember {}` in MainActivity to cache ViewModelFactory instance, preventing unnecessary recreations on recomposition
- **Lifecycle Version Consolidation** - Unified lifecycle dependency versions in libs.versions.toml using single `lifecycle = "2.10.0"` reference instead of separate version entries for runtime, viewmodel-compose, and process artifacts
- **Build Configuration** - Enabled Gradle parallel builds in gradle.properties and added KotlinJvmCompile configuration in app/build.gradle.kts for explicit JVM target specification
- **Error Handling** - Enhanced SoundManager lifecycle error handling to pass exceptions directly to Crashlytics with contextual logging instead of RuntimeException wrapping, preserving original exception types for better diagnostics
- **Code Organization** - Moved companion object placement in QWelcomeApplication to end of class per Kotlin style conventions
- **Import Structure** - Migrated wildcard imports to explicit imports in SettingsScreen for improved code clarity
- **Sealed Class Instances** - Converted `object` to `data object` for UiEvent sealed class instances following Kotlin best practices
- **Bump Script Changelog Handling** - Fixed PowerShell bump-version.ps1 to properly move [Unreleased] entries to version headings using state machine approach, replacing broken regex-based implementation

### Fixed

- **Accessibility** - Added comprehensive content descriptions for all interactive UI elements including buttons, icons, dropdowns, and navigation elements to improve screen reader support
- **Annotation Placement** - Relocated `@OptIn` annotations from file level to function level for better code clarity and reduced annotation scope

### Removed

- **ExampleUnitTest.kt** - Removed unused placeholder test file from test source set

## [2.3.3] - 2026-02-07

### Added

- **Comprehensive Haptic Feedback** - All interactive UI elements now provide tactile feedback for improved user experience:
  - Back navigation buttons across all screens
  - Template list actions (create, edit, duplicate, delete)
  - Export screen checkboxes and export option cards
  - Settings screen buttons (save, discard, check for updates)
  - Customer intake dropdown menu items and network toggle
  - Template variable copy buttons
  - Search field clear button
- **Release Documentation** - Added comprehensive `docs/RELEASE_GUIDE.md` with step-by-step release workflow for AI agents
- **Import Size Validation** - Enhanced import size limit messaging with dedicated `formatBytesAsMb()` helper
- **Enhanced Error Logging** - Added stack trace logging for QR code save/share failures and file save errors for better diagnostics

### Changed

- Consolidated duplicate SecurityException/IOException handling in export file save flow
- Updated Copilot instructions to reference release guide documentation

### Fixed

- **Code Quality** - Removed excessive trailing blank lines in UpdateChecker.kt

## [2.3.1] - 2026-02-02

### Fixed

- **Tech Profile Import Sync** - Settings screen now immediately displays imported tech profile data (name, title, dept) without requiring navigation away and back
- **Legacy JSON Support** - Added backward compatibility for older exports using `"area"` field instead of `"dept"`

### Changed

- Added `LaunchedEffect` in SettingsScreen to sync local state with imported data
- Enhanced `ExportedTechProfile` with fallback logic for area/dept fields

## [2.3.0] - 2026-02-01

### Added

- **Real-time Placeholder Validation** - Template editor shows missing required placeholders as you type
- **Discard Changes Dialog** - Confirmation prompt when dismissing editor with unsaved changes
- **Template Search** - Filter templates by name in the template list
- **Interactive Placeholder Chips** - Tap chips to insert placeholders at cursor position
- **Keyboard Navigation** - Name field → Next → focuses Content field in template editor
- `Template.sortOrder` field for custom template ordering (default pinned at top)
- `Template.tags` field for future categorization features
- `Template.findMissingPlaceholders()` with whitespace-tolerant regex validation
- `Template.hasRequiredPlaceholders()` convenience helper
- `InteractivePlaceholderChip` component with haptic feedback
- `CollapsiblePlaceholderChips` FlowRow component with expand/collapse
- `PlaceholderLabels` object for short display labels
- `TemplateTest` with 16 test cases for placeholder validation
- `docs/WEB_PORTAL_README.md` for companion config generator web tool

### Changed

- **Template Editor Dialog** - Increased height to 85% for comfortable editing
- **Template Cards** - Compact layout with always-visible action icons (edit, duplicate, delete)
- **Settings Screen** - Removed inline template editing, now links to dedicated Templates screen
- `TemplateListViewModel` validates required placeholders before save
- `TemplateListUiState` now includes `searchQuery` and `validationError` fields

### Removed

- Inline template editing from Settings screen (~200 lines of code)
- `safeTruncate()` helper function (no longer needed)

## [2.2.0] - 2026-02-01

### Added

- **Firebase Crashlytics** - Production crash reporting with automatic stack traces (disabled in debug)
- **Open Network WiFi Support** - Generate password-free QR codes for guest networks with `WIFI:T:nopass` format
- **Template Variables Sheet** - Bottom sheet showing all available placeholders with copy-to-clipboard
- **Settings Rate Limiting** - 60-second cooldown on update checks to prevent GitHub API abuse
- **Version Comparator** - Full SemVer 2.0 support with proper pre-release and build metadata handling
- **Build-Logic Convention Plugins** - Gradle convention plugins for centralized Android and Compose configuration
- New string resources for update check feedback and WiFi labels
- `TimeProvider` interface for testable, monotonic time sources
- `FakeTimeProvider` for unit tests with manual time control
- 3 new unit tests for auto-clear and process death scenarios

### Changed

- **BREAKING: Jetpack Navigation Compose** - Replaced manual Screen enum with type-safe `@Serializable` routes
- **Settings UI** - Reorganized with Save button at top, dismissible update notifications, About section at bottom
- **Cyberpunk Theme** - Added disabled state styling for `NeonOutlinedField`, Danger Zone uses `NeonButton` with error glow
- **Password Validation** - Automatically skipped for open networks, enabling password-free WiFi configurations
- **QR Section** - Shows "Open (No Password)" for unsecured networks instead of requiring password
- **Error Handling** - Better browser failure handling and rate-limit detection for update checks
- `CustomerIntakeViewModel` now uses `SavedStateHandle` to persist background timestamp
- `Navigator.copyToClipboard()` now returns `Boolean` to indicate success/failure
- All interval timing uses monotonic clock instead of wall clock

### Fixed

- **Issue 2:** Removed unused `template_var_service_type` string resource
- **Issue 3:** Auto-clear timeout now persists across process death using `SavedStateHandle`
- **Issue 4:** Switched to `SystemClock.elapsedRealtime()` for interval timing (immune to clock changes)
- **Issue 5:** Added error handling and accurate feedback for clipboard operations
- **Rate-Limit Detection** - Only treats HTTP 403 as rate-limited when `X-RateLimit-Remaining: 0` header present
- **Version Comparison** - Strips SemVer build metadata (`+build`) before parsing
- **Lifecycle Awareness** - Settings events now collected with `repeatOnLifecycle` to prevent off-screen toasts
- **ProGuard Rules** - Removed duplicate Firebase Crashlytics attribute rules
- **Build-Logic** - Added package declaration and defensive checks for `ApplicationExtension` availability
- **Tech Signature Formatting** - Consistent newline formatting between placeholder and auto-append paths

### Technical

- 20 comprehensive unit tests for `VersionComparator` (including build metadata handling)
- 4 new tests for settings rate limiting
- Navigation uses `NavController` for automatic back navigation handling
- Convention plugins in `build-logic/` for shared Gradle configuration

## [2.1.0] - 2026-01-30

### Added

- Version bump script (`scripts/bump-version.sh`) for one-command releases
- `version.properties` as single source of truth for app versioning
- `.gitattributes` to enforce LF line endings for scripts

### Changed

- App version is now read dynamically from `version.properties` instead of hardcoded in `build.gradle.kts`
- GitHub Actions release workflow now extracts real changelog content into the release body

### Technical

- ViewModel unit tests with test infrastructure
- Updated dependencies for coroutinesTest, turbine, and mockk
- Improved state collection with `collectAsStateWithLifecycle`
- Cleaned up ProGuard rules and error handling in import/export screens

## [1.0.0] - 2026-01-25

### Added

- Initial release of Q Welcome
- Customer intake form with validation
- SMS, Share, and Copy message options
- WiFi QR code generation
- Cyberpunk-themed UI with dark/light mode
- Custom message templates
- Template import/export functionality
- Tech profile signature support
- Auto-clear security feature (10-minute timeout)
- Rate limiting to prevent accidental double-sends
- Complete customer intake workflow
- Template management system
- Import/Export with JSON schema v1

### Technical

- MVVM architecture with Jetpack Compose
- StateFlow for reactive state management
- DataStore for local persistence
- ProGuard rules for release builds