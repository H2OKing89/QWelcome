package com.kingpaging.qwelcome.viewmodel.settings

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.UpdateChecker
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockStore = mockk<SettingsStore>(relaxed = true)
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

        mockkObject(UpdateChecker)

        vm = SettingsViewModel(mockStore)
    }

    @After
    fun teardown() {
        unmockkObject(UpdateChecker)
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `techProfile flow reflects store data`() = runTest {
        vm.techProfile.test {
            val item = awaitItem()
            // May receive initialValue first, then the real value
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
    fun `checkForUpdate with available update sets Available state`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = "3.0.0",
                    downloadUrl = "https://example.com/app.apk",
                    releaseNotes = "New features"
                )

        vm.checkForUpdate()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Available)
        assertEquals("3.0.0", (state as UpdateState.Available).version)
        assertEquals("https://example.com/app.apk", state.downloadUrl)
    }

    @Test
    fun `checkForUpdate when up to date sets UpToDate state`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns
                UpdateCheckResult.UpToDate

        vm.checkForUpdate()
        advanceUntilIdle()

        assertTrue(vm.updateState.value is UpdateState.UpToDate)
    }

    @Test
    fun `checkForUpdate error sets Error state`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns
                UpdateCheckResult.Error("Network error")

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
    fun `checkForUpdate sets Checking then resolves`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns UpdateCheckResult.UpToDate

        vm.checkForUpdate()
        advanceUntilIdle()

        // Verify the final state is UpToDate (went through Checking → UpToDate)
        assertTrue(vm.updateState.value is UpdateState.UpToDate)
        coVerify(exactly = 1) { UpdateChecker.checkForUpdate(any()) }
    }

    // === COOLDOWN TESTS ===

    @Test
    fun `checkForUpdate within cooldown emits toast event and does not call UpdateChecker`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns UpdateCheckResult.UpToDate

        // First check succeeds
        vm.checkForUpdate()
        advanceUntilIdle()
        assertTrue(vm.updateState.value is UpdateState.UpToDate)

        // Second check within cooldown should emit toast, not call UpdateChecker again
        vm.settingsEvents.test {
            vm.checkForUpdate()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowToast)
            assertTrue((event as SettingsEvent.ShowToast).message.contains("try again in"))
        }

        // UpdateChecker should only have been called once
        coVerify(exactly = 1) { UpdateChecker.checkForUpdate(any()) }
    }

    @Test
    fun `checkForUpdate after cooldown expired proceeds normally`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns UpdateCheckResult.UpToDate

        // First check
        vm.checkForUpdate()
        advanceUntilIdle()

        // Reset cooldown by setting lastCheckTimeMillis to 0
        vm.lastCheckTimeMillis = 0L

        // Second check should proceed
        vm.checkForUpdate()
        advanceUntilIdle()

        coVerify(exactly = 2) { UpdateChecker.checkForUpdate(any()) }
    }

    // === RATE LIMIT TESTS ===

    @Test
    fun `rate limited with retry seconds sets Error state with retry info`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns
                UpdateCheckResult.RateLimited(retryAfterSeconds = 42)

        vm.checkForUpdate()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Error)
        val message = (state as UpdateState.Error).message
        assertTrue(message.contains("42"))
        assertTrue(message.contains("Rate limited"))
    }

    @Test
    fun `rate limited without retry seconds sets Error state with generic message`() = runTest {
        coEvery { UpdateChecker.checkForUpdate(any()) } returns
                UpdateCheckResult.RateLimited(retryAfterSeconds = null)

        vm.checkForUpdate()
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is UpdateState.Error)
        val message = (state as UpdateState.Error).message
        assertTrue(message.contains("Try again later"))
    }
}
