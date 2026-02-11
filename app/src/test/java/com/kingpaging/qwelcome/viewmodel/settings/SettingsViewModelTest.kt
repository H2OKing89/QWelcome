package com.kingpaging.qwelcome.viewmodel.settings

import app.cash.turbine.test
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DownloadEnqueueResult
import com.kingpaging.qwelcome.data.DownloadStatus
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.VerificationResult
import com.kingpaging.qwelcome.testutil.FakeAppUpdater
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
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
        AppViewModelProvider.resetForTesting()
        every { mockStore.techProfileFlow } returns flowOf(testProfile)
        every { mockStore.allTemplatesFlow } returns flowOf(listOf(testTemplate))
        every { mockStore.activeTemplateFlow } returns flowOf(testTemplate)
        every { mockStore.defaultTemplateContent } returns "Default content"

        fakeAppUpdater = FakeAppUpdater()
        vm = SettingsViewModel(mockStore, fakeResourceProvider, fakeAppUpdater)
    }

    @After
    fun teardown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `techProfile flow reflects store data`() = runTest {
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
    fun `allTemplates flow reflects store data`() = runTest {
        vm.allTemplates.test {
            val item = awaitItem()
            if (item.isEmpty()) {
                assertEquals(listOf(testTemplate), awaitItem())
            } else {
                assertEquals(listOf(testTemplate), item)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `activeTemplate flow reflects store data`() = runTest {
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
    fun `save calls store saveTechProfile`() = runTest {
        val newProfile = TechProfile(name = "Jane", title = "Lead Tech", dept = "Eng")
        vm.save(newProfile)
        advanceUntilIdle()

        coVerify { mockStore.saveTechProfile(newProfile) }
    }

    @Test
    fun `save error emits error event`() = runTest {
        coEvery { mockStore.saveTechProfile(any()) } throws RuntimeException("DB error")

        vm.errorEvents.test {
            vm.save(testProfile)
            val error = awaitItem()
            assertTrue(error.contains("Failed to save profile"))
        }
    }

    @Test
    fun `saveTemplate calls store saveTemplate`() = runTest {
        vm.saveTemplate(testTemplate)
        advanceUntilIdle()

        coVerify { mockStore.saveTemplate(testTemplate) }
    }

    @Test
    fun `setActiveTemplate calls store`() = runTest {
        vm.setActiveTemplate("t1")
        advanceUntilIdle()

        coVerify { mockStore.setActiveTemplate("t1") }
    }

    @Test
    fun `deleteTemplate calls store`() = runTest {
        vm.deleteTemplate("t1")
        advanceUntilIdle()

        coVerify { mockStore.deleteTemplate("t1") }
    }

    @Test
    fun `resetTemplate calls store resetToDefaultTemplate`() = runTest {
        vm.resetTemplate()
        advanceUntilIdle()

        coVerify { mockStore.resetToDefaultTemplate() }
    }

    @Test
    fun `getDefaultTemplateContent returns store value`() {
        assertEquals("Default content", vm.getDefaultTemplateContent())
    }

    @Test
    fun `checkForUpdate with available update sets Available state with metadata`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "New features",
            assetName = "app-v3.apk",
            assetSizeBytes = 100L,
            sha256Hex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
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
    fun `checkForUpdate when up to date sets UpToDate state`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

        vm.checkForUpdate()
        advanceUntilIdle()

        assertTrue(vm.updateState.value is UpdateState.UpToDate)
    }

    @Test
    fun `checkForUpdate error sets Error state`() = runTest {
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
    fun `download confirmation flag toggles via viewmodel methods`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Notes",
            assetName = "app-v3.apk",
            assetSizeBytes = 200L,
            sha256Hex = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
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
    fun `checkForUpdate sets Checking then resolves and calls updater once`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

        vm.checkForUpdate()
        advanceUntilIdle()

        assertTrue(vm.updateState.value is UpdateState.UpToDate)
        assertEquals(1, fakeAppUpdater.checkCallCount)
    }

    @Test
    fun `download flow emits queued downloading verifying and ready states`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Notes",
            assetName = "app-v3.apk",
            assetSizeBytes = 200L,
            sha256Hex = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
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
    fun `retryInstallAfterPermission emits launch intent when install is allowed`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Notes",
            assetName = "app-v3.apk",
            assetSizeBytes = 200L,
            sha256Hex = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
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
    fun `retryInstallAfterPermission sets Error when install intent unavailable`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Notes",
            assetName = "app-v3.apk",
            assetSizeBytes = 200L,
            sha256Hex = "abababababababababababababababababababababababababababababababab"
        )
        fakeAppUpdater.enqueueResult = DownloadEnqueueResult.Started(42L, "/tmp/app.apk")
        fakeAppUpdater.downloadStatusQueue.add(DownloadStatus.Succeeded("/tmp/app.apk"))
        fakeAppUpdater.verificationResult = VerificationResult.Success("/tmp/app.apk")
        fakeAppUpdater.installIntent = null

        vm.checkForUpdate()
        advanceUntilIdle()
        vm.startUpdateDownload()
        advanceUntilIdle()

        vm.retryInstallAfterPermission()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Error)
        assertEquals(
            fakeResourceProvider.getString(R.string.error_update_install_unavailable),
            (state as UpdateState.Error).message
        )
    }

    @Test
    fun `retryInstallAfterPermission moves to PermissionRequired when install permission disabled`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Notes",
            assetName = "app-v3.apk",
            assetSizeBytes = 200L,
            sha256Hex = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
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
    fun `checkForUpdate within cooldown emits toast event and does not call updater`() = runTest {
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
    fun `checkForUpdate after cooldown expired proceeds normally`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.UpToDate

        vm.checkForUpdate()
        advanceUntilIdle()

        vm.lastCheckTimeMillis = 0L

        vm.checkForUpdate()
        advanceUntilIdle()

        assertEquals(2, fakeAppUpdater.checkCallCount)
    }

    @Test
    fun `rate limited with retry seconds sets Error state with retry info`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.RateLimited(retryAfterSeconds = 42)

        vm.checkForUpdate()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Error)
        val message = (state as UpdateState.Error).message
        assertTrue(message.contains("42"))
    }

    @Test
    fun `rate limited without retry seconds sets Error state with generic message`() = runTest {
        fakeAppUpdater.checkForUpdateResult = UpdateCheckResult.RateLimited(retryAfterSeconds = null)

        vm.checkForUpdate()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Error)
        val message = (state as UpdateState.Error).message
        assertEquals(fakeResourceProvider.getString(R.string.toast_rate_limited), message)
    }
}
