package com.kingpaging.qwelcome.viewmodel.settings

import app.cash.turbine.test
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DownloadEnqueueResult
import com.kingpaging.qwelcome.data.DownloadStatus
import com.kingpaging.qwelcome.data.PendingUpdateDownload
import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.VerificationResult
import com.kingpaging.qwelcome.testutil.FakeAppUpdater
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockStore = mockk<SettingsStore>(relaxed = true)
    private val fakeResourceProvider = FakeResourceProvider()
    private lateinit var fakeAppUpdater: FakeAppUpdater
    private lateinit var vm: SettingsViewModel

    private val testProfile = TechProfile(name = "John", title = "Sr Tech", dept = "IT")
    private val testTemplate = Template(id = "t1", name = "Test", content = "Hello")

    @Before
    fun setup() {
        every { mockStore.techProfileFlow } returns flowOf(testProfile)
        every { mockStore.privacySettingsFlow } returns flowOf(PrivacySettings())
        every { mockStore.activeTemplateFlow } returns flowOf(testTemplate)
        every { mockStore.defaultTemplateContent } returns "Default content"

        fakeAppUpdater = FakeAppUpdater()
        vm = SettingsViewModel(mockStore, fakeResourceProvider, fakeAppUpdater)
    }

    @Test
    fun `techProfile flow reflects store data`() =
        runTest {
            vm.techProfile.test {
                val item = awaitItem()
                if (item == TechProfile()) {
                    assertEquals(testProfile, awaitItem())
                } else {
                    assertEquals(testProfile, item)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `activeTemplate flow reflects store data`() =
        runTest {
            vm.activeTemplate.test {
                val item = awaitItem()
                if (item.id != testTemplate.id) {
                    assertEquals(testTemplate, awaitItem())
                } else {
                    assertEquals(testTemplate, item)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `save calls store saveTechProfile`() =
        runTest {
            val newProfile = TechProfile(name = "Jane", title = "Lead Tech", dept = "Eng")
            vm.save(newProfile)
            advanceUntilIdle()

            coVerify { mockStore.saveTechProfile(newProfile) }
        }

    @Test
    fun `save emits one profile saved event without replay`() =
        runTest {
            vm.settingsEvents.test {
                vm.save(testProfile)
                assertEquals(SettingsEvent.ProfileSaved, awaitItem())
                expectNoEvents()
            }

            vm.settingsEvents.test {
                expectNoEvents()
            }
        }

    @Test
    fun `setCrashReportingEnabled saves updated privacy settings`() =
        runTest {
            var storedSettings = PrivacySettings(crashReportingEnabled = true)
            coEvery { mockStore.updatePrivacySettings(any()) } coAnswers {
                @Suppress("UNCHECKED_CAST")
                val transform = invocation.args[0] as (PrivacySettings) -> PrivacySettings
                storedSettings = transform(storedSettings)
            }

            vm.setCrashReportingEnabled(false)
            advanceUntilIdle()

            assertEquals(false, storedSettings.crashReportingEnabled)
        }

    @Test
    fun `setScreenCaptureProtectionEnabled saves updated privacy settings`() =
        runTest {
            var storedSettings = PrivacySettings(screenCaptureProtectionEnabled = false)
            coEvery { mockStore.updatePrivacySettings(any()) } coAnswers {
                @Suppress("UNCHECKED_CAST")
                val transform = invocation.args[0] as (PrivacySettings) -> PrivacySettings
                storedSettings = transform(storedSettings)
            }

            vm.setScreenCaptureProtectionEnabled(true)
            advanceUntilIdle()

            assertEquals(true, storedSettings.screenCaptureProtectionEnabled)
        }

    @Test
    fun `privacy setters preserve both changes when invoked before dispatcher advancement`() =
        runTest {
            var storedSettings = PrivacySettings(crashReportingEnabled = true)
            coEvery { mockStore.updatePrivacySettings(any()) } coAnswers {
                @Suppress("UNCHECKED_CAST")
                val transform = invocation.args[0] as (PrivacySettings) -> PrivacySettings
                storedSettings = transform(storedSettings)
            }

            vm.setCrashReportingEnabled(false)
            vm.setScreenCaptureProtectionEnabled(true)
            advanceUntilIdle()

            assertEquals(
                PrivacySettings(crashReportingEnabled = false, screenCaptureProtectionEnabled = true),
                storedSettings,
            )
        }

    @Test
    fun `save error emits settings error event`() =
        runTest {
            coEvery { mockStore.saveTechProfile(any()) } throws RuntimeException("DB error")

            vm.settingsEvents.test {
                vm.save(testProfile)
                val event = awaitItem()
                assertTrue(event is SettingsEvent.ShowToastError)
                assertEquals(
                    fakeResourceProvider.getString(R.string.toast_failed_save_profile),
                    (event as SettingsEvent.ShowToastError).message,
                )
            }
        }

    @Test
    fun `checkForUpdate with available update sets Available state with metadata`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "New features",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 100L,
                    sha256Hex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                )

            vm.checkForUpdate()
            advanceUntilIdle()

            val state = vm.updateState.value
            assertTrue(state is UpdateState.Available)
            state as UpdateState.Available
            assertEquals("3.0.0", state.version)
            assertEquals("app-v3.apk", state.assetName)
            assertEquals(100L, state.assetSizeBytes)
        }

    @Test
    fun `checkForUpdate when up to date sets UpToDate state`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

            vm.checkForUpdate()
            advanceUntilIdle()

            assertTrue(vm.updateState.value is UpdateState.UpToDate)
        }

    @Test
    fun `checkForUpdate error sets Error state`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.Error("Network error")

            vm.checkForUpdate()
            advanceUntilIdle()

            val state = vm.updateState.value
            assertTrue(state is UpdateState.Error)
            assertEquals("Network error", (state as UpdateState.Error).message)
        }

    @Test
    fun `dismissUpdate sets Dismissed state`() {
        vm.dismissUpdate()
        assertTrue(vm.updateState.value is UpdateState.Dismissed)
    }

    @Test
    fun `download confirmation flag toggles via viewmodel methods`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "Notes",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 200L,
                    sha256Hex = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                )
            vm.checkForUpdate()
            advanceUntilIdle()

            assertTrue(!vm.showDownloadConfirmDialog.value)

            vm.requestDownloadConfirmation()
            assertTrue(vm.showDownloadConfirmDialog.value)

            vm.dismissDownloadConfirmation()
            assertTrue(!vm.showDownloadConfirmDialog.value)
        }

    @Test
    fun `checkForUpdate sets Checking then resolves and calls updater once`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

            vm.checkForUpdate()
            advanceUntilIdle()

            assertTrue(vm.updateState.value is UpdateState.UpToDate)
            assertEquals(1, fakeAppUpdater.checkCallCount)
        }

    @Test
    fun `download flow emits queued downloading verifying and ready states`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "Notes",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 200L,
                    sha256Hex = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                )
            fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.InProgress(20L, 100L))
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")

            vm.checkForUpdate()
            advanceUntilIdle()

            vm.startUpdateDownload()
            advanceUntilIdle()

            assertTrue(vm.updateState.value is UpdateState.ReadyToInstall)
            assertEquals("/tmp/app.apk", fakeAppUpdater.lastVerifiedApkPath)
        }

    @Test
    fun `retryInstallAfterPermission emits launch intent when install is allowed`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "Notes",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 200L,
                    sha256Hex = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
                )
            fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")

            vm.checkForUpdate()
            advanceUntilIdle()
            vm.startUpdateDownload()
            advanceUntilIdle()

            vm.settingsEvents.test {
                vm.retryInstallAfterPermission()
                val event = awaitItem()
                assertTrue(event is SettingsEvent.LaunchIntent)
                assertTrue(vm.updateState.value is UpdateState.Installing)
            }
        }

    @Test
    fun `retryInstallAfterPermission retains APK when install intent unavailable`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "Notes",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 200L,
                    sha256Hex = "abababababababababababababababababababababababababababababababab",
                )
            fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")
            fakeAppUpdater.installIntent = null

            vm.checkForUpdate()
            advanceUntilIdle()
            vm.startUpdateDownload()
            advanceUntilIdle()

            vm.settingsEvents.test {
                vm.retryInstallAfterPermission()
                advanceUntilIdle()

                val state = vm.updateState.value as UpdateState.ReadyToInstall
                assertEquals("/tmp/app.apk", state.apkPath)
                val event = awaitItem() as SettingsEvent.ShowToastError
                assertEquals(
                    fakeResourceProvider.getString(R.string.error_update_install_unavailable),
                    event.message,
                )
            }
        }

    @Test
    fun `retryInstallAfterPermission moves to PermissionRequired when install permission disabled`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult =
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "Notes",
                    assetName = "app-v3.apk",
                    assetSizeBytes = 200L,
                    sha256Hex = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                )
            fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")
            fakeAppUpdater.canRequestPackageInstallsValue = false

            vm.checkForUpdate()
            advanceUntilIdle()
            vm.startUpdateDownload()
            advanceUntilIdle()

            vm.retryInstallAfterPermission()
            advanceUntilIdle()

            assertTrue(vm.updateState.value is UpdateState.PermissionRequired)
            val intent = vm.openUnknownSourcesSettingsIntent()
            assertNotNull(intent)
        }

    @Test
    fun `installer cancellation preserves verified APK and allows retry`() =
        runTest {
            prepareDownloadedUpdate()

            vm.settingsEvents.test {
                vm.retryInstallAfterPermission()
                assertTrue(awaitItem() is SettingsEvent.LaunchIntent)
                vm.onInstallerReturned()

                val ready = vm.updateState.value as UpdateState.ReadyToInstall
                assertEquals("/tmp/app.apk", ready.apkPath)
                vm.retryInstallAfterPermission()
                assertTrue(awaitItem() is SettingsEvent.LaunchIntent)
            }
            assertEquals(2, fakeAppUpdater.createInstallIntentCallCount)
            assertEquals(1, fakeAppUpdater.enqueueCallCount)
        }

    @Test
    fun `failed installer launch restores ready state and reports error`() =
        runTest {
            prepareDownloadedUpdate()
            vm.settingsEvents.test {
                vm.retryInstallAfterPermission()
                assertTrue(awaitItem() is SettingsEvent.LaunchIntent)

                vm.onInstallerLaunchFailed()
                assertTrue(awaitItem() is SettingsEvent.ShowToastError)
                assertEquals("/tmp/app.apk", (vm.updateState.value as UpdateState.ReadyToInstall).apkPath)
            }
        }

    @Test
    fun `restoration reconnects to completed download and verifies again`() =
        runTest {
            val update = availableUpdate()
            fakeAppUpdater.pendingDownload = PendingUpdateDownload(42L, "/tmp/app.apk", update)
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.InProgress(10L, 100L))
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")

            vm.restorePendingUpdate()
            advanceUntilIdle()

            assertEquals(0, fakeAppUpdater.enqueueCallCount)
            assertEquals(update, fakeAppUpdater.lastVerifiedUpdate)
            assertEquals("/tmp/app.apk", (vm.updateState.value as UpdateState.ReadyToInstall).apkPath)
            vm.restorePendingUpdate()
            assertEquals(1, fakeAppUpdater.restoredDownloadCount)
        }

    @Test
    fun `stale dismiss and check callbacks cannot interrupt an active download`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = availableUpdate()
            fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.InProgress(10L, 100L))
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
            fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")
            vm.checkForUpdate()
            advanceUntilIdle()
            vm.startUpdateDownload()
            runCurrent()

            vm.dismissUpdate()
            vm.lastCheckTimeMillis = 0L
            vm.checkForUpdate()

            assertTrue(vm.updateState.value is UpdateState.Downloading)
            assertEquals(1, fakeAppUpdater.checkCallCount)
            advanceUntilIdle()
            assertTrue(vm.updateState.value is UpdateState.ReadyToInstall)
        }

    @Test
    fun `failed restored download is discarded`() =
        runTest {
            fakeAppUpdater.pendingDownload = PendingUpdateDownload(42L, "/tmp/app.apk", availableUpdate())
            fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Failed("missing"))

            vm.restorePendingUpdate()
            advanceUntilIdle()

            assertTrue(vm.updateState.value is UpdateState.Error)
            assertEquals(42L, fakeAppUpdater.lastForgottenDownloadId)
        }

    private fun TestScope.prepareDownloadedUpdate() {
        fakeAppUpdater.checkForUpdateResult = availableUpdate()
        fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
        fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
        fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")
        vm.checkForUpdate()
        advanceUntilIdle()
        vm.startUpdateDownload()
        advanceUntilIdle()
    }

    private fun availableUpdate() =
        UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome.apk",
            releaseNotes = "notes",
            assetName = "QWelcome.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64),
        )

    @Test
    fun `checkForUpdate within cooldown emits toast event and does not call updater`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

            vm.checkForUpdate()
            advanceUntilIdle()
            assertTrue(vm.updateState.value is UpdateState.UpToDate)

            vm.settingsEvents.test {
                vm.checkForUpdate()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is SettingsEvent.ShowToastError)
                val message = (event as SettingsEvent.ShowToastError).message
                val expectedPrefix = "${fakeResourceProvider.getString(R.string.toast_check_cooldown)}["
                assertTrue(message.startsWith(expectedPrefix))
            }

            assertEquals(1, fakeAppUpdater.checkCallCount)
        }

    @Test
    fun `checkForUpdate after cooldown expired proceeds normally`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

            vm.checkForUpdate()
            advanceUntilIdle()

            vm.lastCheckTimeMillis = 0L

            vm.checkForUpdate()
            advanceUntilIdle()

            assertEquals(2, fakeAppUpdater.checkCallCount)
        }

    @Test
    fun `rate limited with retry seconds sets Error state with retry info`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.RateLimited(retryAfterSeconds = 42)

            vm.checkForUpdate()
            advanceUntilIdle()

            val state = vm.updateState.value
            assertTrue(state is UpdateState.Error)
            val message = (state as UpdateState.Error).message
            assertTrue(message.contains("42"))
        }

    @Test
    fun `rate limited without retry seconds sets Error state with generic message`() =
        runTest {
            fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.RateLimited(retryAfterSeconds = null)

            vm.checkForUpdate()
            advanceUntilIdle()

            val state = vm.updateState.value
            assertTrue(state is UpdateState.Error)
            val message = (state as UpdateState.Error).message
            assertEquals(fakeResourceProvider.getString(R.string.toast_rate_limited), message)
        }
}
