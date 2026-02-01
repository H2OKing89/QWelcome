# Changelog

All notable changes to Q Welcome will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- **Issue 2:** Removed unused `template_var_service_type` string resource
- **Issue 3:** Auto-clear timeout now persists across process death using `SavedStateHandle`
- **Issue 4:** Switched to `SystemClock.elapsedRealtime()` for interval timing (immune to clock changes)
- **Issue 5:** Added error handling and accurate feedback for clipboard operations

### Added

- `TimeProvider` interface for testable, monotonic time sources
- `FakeTimeProvider` for unit tests with manual time control
- 3 new unit tests for auto-clear and process death scenarios

### Changed

- `CustomerIntakeViewModel` now uses `SavedStateHandle` to persist background timestamp
- `Navigator.copyToClipboard()` now returns `Boolean` to indicate success/failure
- All interval timing uses monotonic clock instead of wall clock

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
