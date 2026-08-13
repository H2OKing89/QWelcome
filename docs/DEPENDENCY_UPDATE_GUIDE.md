# Dependency Update Guide

Dependencies are intentionally updated through reviewable pull requests. Dependabot opens weekly grouped pull requests for Gradle dependencies and GitHub Actions; it does not auto-merge them.

## Sources of Truth

- Gradle library and plugin versions live in `gradle/libs.versions.toml`.
- The Gradle wrapper version lives in `gradle/wrapper/gradle-wrapper.properties`.
- Application release versions live only in `version.properties`.
- GitHub Action versions are declared directly in `.github/workflows/`.
- Compose library versions come from the Compose BOM. Do not add independent versions to individual Compose artifacts.

## Safe Update Procedure

1. Start a feature branch and review the dependency's release notes, migration notes, and known compatibility requirements.
2. Update one compatible group at a time. Keep Kotlin, the Compose compiler plugin, Android Gradle Plugin, Gradle, and JDK compatibility in view together.
3. For a Dependabot pull request, inspect the complete diff before merging. Grouped updates still need individual compatibility review.
4. Run the smallest relevant check first. For example, use `:app:assembleDebug` for an Android Gradle Plugin or Compose change, and `:app:detekt` for a Detekt update.
5. Run the full local gate before merging:

```bash
./gradlew :app:testDebugUnitTest \
  :app:compileDebugAndroidTestKotlin \
  :app:ktlintCheck \
  :app:detekt \
  :app:lintDebug \
  :app:assembleDebug
```

1. Run connected tests when the update affects AndroidX, Compose, navigation, DataStore, or test tooling:

```bash
./gradlew :app:connectedDebugAndroidTest
```

1. Record user-visible behavior or compatibility changes in `CHANGELOG.md` under `Unreleased`.

## Compatibility-Sensitive Groups

### Android Build Tooling

Treat Kotlin, the Kotlin Compose plugin, Android Gradle Plugin, Gradle wrapper, and JDK as a compatibility set. CI uses JDK 17 while the application targets Java and Kotlin bytecode level 11. Check the Android Gradle Plugin compatibility table before changing the wrapper or JDK requirement.

### Compose

The Compose BOM controls the versions of Compose UI, foundation, Material 3, testing, and tooling artifacts. Update the BOM first, retain versionless Compose dependency declarations, and run both lint and connected tests because API or rendering changes can affect Compose behavior.

### AndroidX Test

Keep the `androidx.test.services:test-services` utility APK aligned with the `androidx.test.services:storage` version pulled transitively by AndroidX Test Runner. Verify the resolved storage version after updating JUnit, Espresso, Compose test libraries, or AGP:

```bash
./gradlew :app:dependencyInsight \
  --dependency androidx.test.services:storage \
  --configuration debugAndroidTestRuntimeClasspath \
  --single-path
```

Run a connected test after changing this group. Without the matching utility APK in `androidTestUtil`, AGP's Unified Test Platform attempts to grant an app-op before `androidx.test.services` has a device UID.

### Proto and DataStore

The repository uses the `com.google.protobuf` Gradle plugin and a checked-in `.proto` schema. For a major schema or plugin change:

```bash
./gradlew clean
./gradlew :proto:build :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```

Never reuse a Proto field number for a different meaning. Reserve removed field numbers in the schema. Treat Gradle 10 compatibility warnings from the protobuf toolchain as plugin-compatibility work; do not work around them with platform-specific artifact strings.

### GitHub Actions

Review action updates for changed inputs, permissions, runtime requirements, and deprecations. The Android workflow relies on a composite debug Google Services action and an emulator action, so validate workflow YAML and inspect the action documentation when either changes.

## Reviewing Dependabot Pull Requests

- Confirm the package and target version match the intended ecosystem.
- Read breaking-change notes, not only the changelog heading.
- Check that generated sources and lock-free Gradle resolution remain reproducible.
- Do not combine an unrelated product change with a dependency update.
- Merge only after required CI jobs and the local verification gate pass.

## Rollback

If an updated dependency breaks build, lint, tests, or device behavior, revert only that update pull request or commit. Do not change application code merely to mask an unexplained dependency regression. Capture the incompatible version and the failing task in the pull request before retrying with a compatible version.
