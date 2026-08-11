# Troubleshooting Guide

## Build Setup

Use JDK 17 or newer with Android SDK 36 installed. The app targets Java and Kotlin bytecode level 11, but Gradle and CI run on JDK 17.

```bash
chmod +x gradlew
./gradlew :app:assembleDebug
```

For a complete local check, run:

```bash
./gradlew :app:testDebugUnitTest \
  :app:compileDebugAndroidTestKotlin \
  :app:ktlintCheck \
  :app:detekt \
  :app:lintDebug \
  :app:assembleDebug
```

Use `./gradlew --warning-mode all <task>` when Gradle reports deprecations. The current protobuf toolchain may emit Gradle 10 compatibility warnings; identify the plugin or dependency source before changing build scripts.

## Google Services Errors

The repository includes a non-production Google Services configuration for debug builds. CI creates its own debug configuration through `.github/actions/create-google-services`.

For a local release build, provide a production `app/google-services.json` and signing material. Do not commit production Firebase configuration or keystores.

If `processDebugGoogleServices` or `processReleaseGoogleServices` fails:

1. Confirm the JSON file is at `app/google-services.json` for release builds or `app/src/debug/google-services.json` for debug builds.
2. Confirm the JSON contains an Android client whose package name is `com.kingpaging.qwelcome`.
3. Run `./gradlew :app:processDebugGoogleServices` or `./gradlew :app:processReleaseGoogleServices` to isolate the failure.

## Proto DataStore and Generated Sources

After a significant change to `proto/src/main/proto/user_preferences.proto`, clear generated outputs before compiling:

```bash
./gradlew clean
./gradlew :proto:build :app:assembleDebug
```

Keep field numbers stable. Add removed field numbers to `reserved`; reusing them can misinterpret persisted user data. If a migration test fails, run `:app:testDebugUnitTest` and the focused `PreferencesToProtoMigrationTest` before modifying production migration logic.

## Connected Device and Emulator Tests

Check that a device or emulator is visible before running instrumentation tests:

```bash
adb devices
./gradlew :app:connectedDebugAndroidTest
```

CI runs a focused emulator smoke suite on pull requests and pushes, while scheduled or manually dispatched runs execute the full suite. Test reports are uploaded as workflow artifacts after every CI test job.

If an individual Compose test is unstable on a physical device, run its class in a fresh instrumentation process before treating it as a code regression. The Gradle task supports test filtering:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.kingpaging.qwelcome.ui.templates.TemplateEditorScreenTest
```

## Installation and Signing Problems

An `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error means the installed app was signed with a different certificate. Use the same signing key for an update. For a development-only reinstall, uninstall the existing package first; this deletes its local data:

```bash
adb uninstall com.kingpaging.qwelcome
adb install app/build/outputs/apk/debug/app-debug.apk
```

For local release builds, configure either the repository-root `qwelcome-release.keystore` or `KEYSTORE_FILE`, plus these environment variables:

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS` (defaults to `qwelcome`)
- `KEY_PASSWORD`

The release workflow additionally requires `GOOGLE_SERVICES_JSON_BASE64` and `KEYSTORE_BASE64` repository secrets. It fails before signing if the pushed `vX.Y.Z` tag does not match `VERSION_NAME` or if `CHANGELOG.md` lacks that released version.

## Release and Versioning Problems

Use `scripts/bump-version.sh` or `scripts/bump-version.ps1` to update `version.properties`, move the `Unreleased` changelog entries, create the release commit, and create an annotated tag. Follow [the release guide](RELEASE_GUIDE.md) for the required branch and retagging order.

If the release workflow stops before signing, inspect the metadata error first:

- A tag must use the `vX.Y.Z` form.
- The tag version must equal `VERSION_NAME` in `version.properties`.
- `CHANGELOG.md` must have a non-empty `## [X.Y.Z]` section.

Correct the release branch, merge it, recreate the annotated tag on the final default-branch commit, and push the corrected tag. Do not bypass metadata checks by editing the workflow.
