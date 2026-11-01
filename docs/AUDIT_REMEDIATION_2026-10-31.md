# App audit remediation — 2026-10-31

The audit's twelve findings and supporting refactors have been implemented. Existing manual dependency injection, Route/Screen boundaries, Proto DataStore, and focused services are retained. The earlier all-caps customer-name fix remains intact.

## Findings and changes

| Audit finding | Implemented behavior | Regression coverage |
| --- | --- | --- |
| QR crash after optional-password message validation | QR eligibility evaluates shared WiFi validators directly. Message requirements no longer erase invalid credential feedback. | CustomerIntakeUiStateTest; CustomerIntakeViewModelTest |
| Repeated crash on malformed legacy templates | StartupCoordinator gates feature collectors; the recovery screen offers retry, JSON export, and confirmed continuation. Original JSON is archived atomically before skipping templates; the technician profile is preserved. | StartupCoordinatorTest; StartupRecoveryScreenTest; PreferencesToProtoMigrationTest |
| Default-only backups cannot restore | Full backups permit zero custom templates. | ImportValidationRegressionTest; real backup smoke journey |
| Invalid imported IDs overwrite or break templates | Shared structure validation rejects blank, duplicate, and reserved IDs, plus blank names/content, before import application or persistence. | ImportValidationRegressionTest; SettingsStoreSelectionTest |
| QR shares reuse earlier recipient URIs | Every share has a unique immutable file/URI; files expire after 24 hours. Startup no longer deletes current shares. | QrCodeShareCacheTest; QrCodeImageServiceTest |
| Rotation loses profile drafts | Saved draft ownership preserves edits through recreation and initial profile loading. | SettingsProfileDraftTest; UiDraftRestorationTest |
| Repeated Save creates duplicate templates | Synchronous in-flight/completed guards and saving UI state prevent duplicate submissions and allow retry after failure. | TemplateEditorViewModelTest |
| Flexible signature placeholders lose signatures | Message generation uses the shared placeholder matcher. | CustomerIntakeViewModelTest |
| Installer cancellation leaves Installing state | The verified APK path is retained and installer return/failure restores an actionable install state. | SettingsViewModelTest |
| Import warnings are hidden | Confirmation renders localized schema/content/placeholder warnings; structurally invalid imports are blocked. | ImportConfirmStepTest; ImportValidationRegressionTest |
| Valid key rotation is rejected | APK identity distinguishes current signers from verified certificate history; forward lineage and exact simultaneous-signer matching have separate rules. | ApkSigningIdentityTest; GitHubAppUpdaterTest |
| Large JSON work runs on Main | Repository operations use an injectable computation dispatcher. Provider reads/writes stay on IO. | ImportValidationRegressionTest |

## Supporting improvements

- Message actions capture one form snapshot before suspension, including the SMS recipient; delayed storage/profile loading cannot mix customers.
- QrCodeImageService owns image allocation, gallery publishing, share-file preparation and cleanup on the producing IO dispatcher.
- Real decoding tests exposed poor contrast in the original neon QR modules. Preview and PNG now share opaque black/white modules and a quiet zone; neon framing stays outside the code.
- QR content scrolls in short windows and adapts its preview size. A compact-height, 2× font-scale test verifies access to actions.
- Intake, settings and export share a centered, keyboard-aware form wrapper capped at 720dp.
- Template editor text has one authoritative TextFieldState with a one-way saved-state mirror; Save reads current text.
- Template-library restoration is preserved; scrolling to top requires an actual loaded selection change.
- SSID/password keyboard behavior is explicit.
- Pending update metadata is stored atomically outside backup and restored/reverified after process recreation. Old APK cleanup protects the active artifact.
- Unused storage-read permission was removed.
- Fake updater code is shared by test source sets rather than packaged as debug application code.
- CI covers Android APIs 28, 30 and 36. A debug-signed smoke build inherits release R8/resource shrinking and disables Crashlytics mapping/symbol uploads.
- Architecture documentation and the separate security remediation/diff report are updated.

## Verification

- Full debug unit suite: passed.
- Production and debug instrumentation compilation: passed.
- ktlint and Detekt: passed.
- Android Lint: passed in a fresh standalone process.
- Debug APK assembly: passed.
- API 37 emulator regression batch: 89 passed on the first run; the two QR decoding failures and a JUnit setup return-type error were corrected. All six QR/smoke cases passed on the focused rerun.
- Eight additional settings/update-dialog and export screen tests: passed.
- Minified smoke suite on the API 37 emulator: all 10 tests passed, covering actual QR decoding, typed navigation, destination ViewModel scopes, and real backup restoration.

The earlier combined analysis run stalled/crashed inside Kotlin PSI/FIR analysis. Standalone Lint passed with a fresh Gradle process; no lint rules were disabled.

## Repeatable commands

Run analysis tools in separate invocations when diagnosing shared analysis-process failures:

```text
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
./gradlew :app:ktlintCheck :app:detekt
./gradlew :app:lintDebug --no-daemon --max-workers=1
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:connectedSmokeAndroidTest -PtestBuildType=smoke -Pandroid.testInstrumentationRunnerArguments.class=com.kingpaging.qwelcome.smoke.ReleaseSmokeTest,com.kingpaging.qwelcome.navigation.DestinationViewModelScopeTest,com.kingpaging.qwelcome.data.QrCodeImageServiceTest
```

Select an isolated emulator with ANDROID_SERIAL when a physical device is also connected. The smoke APK uses a debug signing key and is intended for verification; production release signing is unchanged. Smoke-only shared-library ABI retention is documented in app/proguard-smoke.pro. Test fixtures are registered through AGP's Kotlin source sets so they are available to both debug and smoke instrumentation.

## Practical validation limits

Smoke-only rules retain the shared library ABI used by the separate instrumentation APK. The smoke run validates application minification and resource shrinking; it does not establish release-level pruning of shared library APIs. This AGP limitation is tracked in [issue 126429384](https://issuetracker.google.com/issues/126429384).

Unit tests exercise installer cancellation, restored downloads and signer policy with fakes. Actual OEM installer/unknown-source return behavior, OS process death during a real download, and a real signing-rotation APK still require platform integration checks. CI's API 28/30/36 matrix is configured but has not been executed remotely from this workspace.
