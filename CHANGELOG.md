# Changelog

All notable changes to Q Welcome will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

No unreleased changes.

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
