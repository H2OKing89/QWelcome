# Security and updater remediation

This report covers the security and updater changes from the app audit. It does not claim that every platform path has been exercised on a device.

## Areas, priorities, and resulting behavior

| Area | Priority | Change and impact |
| --- | --- | --- |
| APK signing identity | Medium | Distinguishes simultaneous signers from key rotation. A multi-signed APK requires identical current signers. A single-signed APK can move forward through the archive's verified signing lineage, but cannot roll back to an old key. Existing SHA-256 and package-name validation remain mandatory. |
| Installer lifecycle | Medium | Retains the verified APK path while handing off to the installer. Cancellation, return, and launch failure restore an actionable install state instead of leaving the app displaying Installing indefinitely. |
| Download recovery | Medium | Saves the DownloadManager ID and exact release/digest metadata atomically outside Android backup. Reopening settings after process recreation reconnects to the download and verifies completed bytes again. |
| APK retention | Low | Removes APKs older than seven days from the app's dedicated updates directory, while protecting the active update. Once the installed version reaches the pending update version, its download record and DownloadManager entry are removed. |
| Least-privilege permissions | Low | Removes the unused READ_EXTERNAL_STORAGE declaration. Legacy write permission remains limited to Android 9 and earlier for gallery saving. |

## Files changed

Production:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/kingpaging/qwelcome/data/ApkSigningIdentity.kt` (new)
- `app/src/main/java/com/kingpaging/qwelcome/data/UpdateDownloadStore.kt` (new)
- `app/src/main/java/com/kingpaging/qwelcome/data/AppUpdater.kt`
- `app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt`
- `app/src/main/java/com/kingpaging/qwelcome/data/UpdateChecker.kt`
- `app/src/main/java/com/kingpaging/qwelcome/viewmodel/settings/SettingsViewModel.kt`
- `app/src/main/java/com/kingpaging/qwelcome/ui/settings/SettingsRoute.kt`
- `app/src/main/java/com/kingpaging/qwelcome/ui/settings/SettingsUpdateSection.kt`

Verification:

- `app/src/test/java/com/kingpaging/qwelcome/data/ApkSigningIdentityTest.kt` (new)
- `app/src/test/java/com/kingpaging/qwelcome/data/PendingUpdateDownloadTest.kt` (new)
- `app/src/test/java/com/kingpaging/qwelcome/data/MemoryUpdateDownloadStore.kt` (new test helper)
- `app/src/test/java/com/kingpaging/qwelcome/data/GitHubAppUpdaterTest.kt`
- `app/src/test/java/com/kingpaging/qwelcome/viewmodel/settings/SettingsViewModelTest.kt`
- `app/src/androidTest/java/com/kingpaging/qwelcome/data/UpdateDownloadStoreTest.kt` (new)
- Shared `FakeAppUpdater` test fixture, including pending-download recovery support.

No new library dependencies or user-facing permissions were added.

## Implementation details and targeted unified diff excerpts

The excerpts below show the central changes. The workspace diff contains the complete implementation and regression tests.

### APK verification

The old subset comparison incorrectly treated proof-of-rotation certificates as simultaneous signers. Verification now compares identities with current signers separate from certificate history.

```diff
--- a/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
+++ b/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
@@
-            val installedSigners = getSignerFingerprints(installedInfo)
-            val archiveSigners = getSignerFingerprints(archiveInfo)
-            if (!signerSetsMatch(installedSigners, archiveSigners)) {
+            val installedSigners = getSigningIdentity(installedInfo)
+            val archiveSigners = getSigningIdentity(archiveInfo)
+            if (!installedSigners.acceptsUpdate(archiveSigners)) {
@@
-    internal fun signerSetsMatch(
-        installedSigners: Set<String>,
-        archiveSigners: Set<String>,
-    ): Boolean {
-        if (installedSigners.isEmpty() || archiveSigners.isEmpty()) return false
-        return installedSigners.containsAll(archiveSigners)
-    }
--- /dev/null
+++ b/app/src/main/java/com/kingpaging/qwelcome/data/ApkSigningIdentity.kt
@@
+internal data class ApkSigningIdentity(
+    val currentSigners: Set<String>,
+    val certificateHistory: Set<String> = currentSigners,
+) {
+    fun acceptsUpdate(archive: ApkSigningIdentity): Boolean =
+        when {
+            currentSigners.isEmpty() || archive.currentSigners.isEmpty() -> false
+            currentSigners.size > 1 || archive.currentSigners.size > 1 -> currentSigners == archive.currentSigners
+            else ->
+                currentSigners == archive.currentSigners ||
+                    archive.certificateHistory.containsAll(currentSigners)
+        }
+}
```

History is obtained from Android's `SigningInfo.signingCertificateHistory` only for a single-signer APK. Multiple simultaneous signers use `apkContentsSigners`. Missing signing information fails closed. Android remains the final authority when installing an APK.

Reference: [SigningInfo](https://developer.android.com/reference/android/content/pm/SigningInfo).

### Installer handoff and retry

The installer intent is launched through the activity-result API. Removing NEW_TASK allows that result contract to work. Lifecycle resume is a fallback for installers that do not reliably return a result. Recovery only changes Installing; returning from unknown-source settings leaves PermissionRequired available for the existing explicit retry action.

```diff
--- a/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
+++ b/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
@@
                 addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
-                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
+                putExtra(Intent.EXTRA_RETURN_RESULT, true)
--- a/app/src/main/java/com/kingpaging/qwelcome/viewmodel/settings/SettingsViewModel.kt
+++ b/app/src/main/java/com/kingpaging/qwelcome/viewmodel/settings/SettingsViewModel.kt
@@
-            _updateState.value = UpdateState.Installing(version)
+            _updateState.value = UpdateState.Installing(version, apkPath)
@@
     data class Installing(
         val version: String,
+        val apkPath: String,
     ) : UpdateState()
@@
+    fun onInstallerReturned() {
+        val installing = _updateState.value as? UpdateState.Installing ?: return
+        _updateState.value = UpdateState.ReadyToInstall(installing.version, installing.apkPath)
+    }
--- a/app/src/main/java/com/kingpaging/qwelcome/ui/settings/SettingsRoute.kt
+++ b/app/src/main/java/com/kingpaging/qwelcome/ui/settings/SettingsRoute.kt
@@
+    val installerLauncher =
+        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
+            viewModel.onInstallerReturned()
+        }
@@
-                        launchIntent(context, event.intent, launchIntentFailedMessage)
+                        try {
+                            installerLauncher.launch(event.intent)
+                        } catch (_: ActivityNotFoundException) {
+                            viewModel.onInstallerLaunchFailed()
+                        } catch (_: SecurityException) {
+                            viewModel.onInstallerLaunchFailed()
+                        } catch (_: IllegalArgumentException) {
+                            viewModel.onInstallerLaunchFailed()
+                        }
```

The ViewModel also ignores update-check/dismiss callbacks during checking, downloading, verification, and installation, and marks download work queued before launching its coroutine to prevent duplicate requests.

### Durable metadata and cleanup

`FileUpdateDownloadStore` stores `pending_update.json` under `Context.noBackupFilesDir`. It uses `AtomicFile` for replacement and discards malformed serialized records. File access is lazy and occurs on the updater's I/O dispatcher. Stored metadata contains release information and a local APK path; it contains no customer names, phone numbers, account numbers, or Wi-Fi credentials.

```diff
--- /dev/null
+++ b/app/src/main/java/com/kingpaging/qwelcome/data/UpdateDownloadStore.kt
@@
+@Serializable
+data class PendingUpdateDownload(
+    val downloadId: Long,
+    val apkPath: String,
+    val update: UpdateCheckResult.UpdateAvailable,
+)
@@
+    override fun write(download: PendingUpdateDownload) {
+        val bytes = json.encodeToString(download).toByteArray(Charsets.UTF_8)
+        val stream = file.startWrite()
+        try {
+            stream.write(bytes)
+            file.finishWrite(stream)
+        } catch (exception: IOException) {
+            file.failWrite(stream)
+            throw exception
+        }
+    }
--- a/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
+++ b/app/src/main/java/com/kingpaging/qwelcome/data/GitHubAppUpdater.kt
@@
                 val downloadId = downloadManager.enqueue(request)
                 val apkPath = destinationFile.absolutePath
+                try {
+                    pendingDownloadStore.write(PendingUpdateDownload(downloadId, apkPath, update))
+                } catch (exception: java.io.IOException) {
+                    downloadManager.remove(downloadId)
+                    throw exception
+                }
                 downloadPaths[downloadId] = apkPath
```

Restoration runs once when settings is entered. It checks the installed version, reconnects to an outstanding ID, queries DownloadManager, and re-runs full APK verification before exposing Install. Failed/missing downloads are forgotten. Retention visits only direct regular APK files inside the app-specific updates directory, never directories or unrelated file types, and excludes the active path.

There is an unavoidable small interval between enqueueing a system DownloadManager request and durably writing its ID; failure during the write cancels that request, while abrupt process termination in that interval may leave an untracked system download. It will not be installed without verification.

### Manifest cleanup

```diff
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@
-    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
-        android:maxSdkVersion="32" />
```

## Verification and practical limits

The full debug unit suite passed, including signer policy, update state recovery, metadata persistence, and retention regressions. Instrumented AtomicFile tests passed in the API 37 emulator regression batch. Production and instrumented compilation, Android Lint, and the ten-case minified smoke suite passed. See [the full remediation report](AUDIT_REMEDIATION_2026-10-31.md) for the complete verification scope and test-harness limitations.

Added regression coverage checks:

- Unchanged single keys and forward key rotation are accepted.
- Old historical keys, missing signatures, and mismatched simultaneous signer sets are rejected.
- Persisted metadata retains the exact SHA-256 and release details.
- Recreated updater/ViewModel instances reconnect without re-enqueueing and reverify completed downloads.
- Missing downloads are forgotten; installed updates remove their DownloadManager entry.
- Cancellation and failed installer launch preserve the verified path and allow retry.
- Stale dismiss/check callbacks cannot interrupt an active download.
- Retention removes old unused APKs while preserving the active APK, recent files, other types, and directories.
- Instrumented AtomicFile tests cover store recreation, clearing, and malformed-record handling.

Device validation should include installer cancellation, unknown-source permission return, process termination during a download, completed-download restoration, and a real APK signed using a valid rotation lineage. These platform scenarios are not fully proven by helper unit tests alone.
