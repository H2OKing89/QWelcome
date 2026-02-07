package com.kingpaging.qwelcome.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.FakeTimeProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerIntakeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockStore = mockk<SettingsStore>(relaxed = true)
    private val fakeResourceProvider = FakeResourceProvider()
    private val fakeTimeProvider = FakeTimeProvider(10000L) // Start at 10 seconds
    private val savedStateHandle = SavedStateHandle()
    private lateinit var vm: CustomerIntakeViewModel

    private val testTemplate = Template(
        id = "550e8400-e29b-41d4-a716-446655440000",
        name = "Test",
        content = "Hello {{ customer_name }}, SSID: {{ ssid }}, PW: {{ password }}, Acct: {{ account_number }}"
    )

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        every { mockStore.techProfileFlow } returns flowOf(TechProfile("Tech", "Sr Tech", "IT"))
        every { mockStore.activeTemplateFlow } returns flowOf(testTemplate)
        vm = CustomerIntakeViewModel(
            savedStateHandle = savedStateHandle,
            settingsStore = mockStore,
            resourceProvider = fakeResourceProvider,
            timeProvider = fakeTimeProvider
        )
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `onCustomerNameChanged updates state and clears error`() {
        vm.onCustomerNameChanged("Alice")
        assertEquals("Alice", vm.uiState.value.customerName)
        assertNull(vm.uiState.value.customerNameError)
    }

    @Test
    fun `onCustomerPhoneChanged updates state with progressive validation`() {
        vm.onCustomerPhoneChanged("555")
        assertEquals("555", vm.uiState.value.customerPhone)
        // Progressive mode shows partial feedback
        assertNotNull(vm.uiState.value.customerPhoneError)
    }

    @Test
    fun `onCustomerPhoneChanged clears error for valid 10-digit number`() {
        vm.onCustomerPhoneChanged("2125551234")
        assertEquals("2125551234", vm.uiState.value.customerPhone)
        assertNull(vm.uiState.value.customerPhoneError)
    }

    @Test
    fun `onSsidChanged updates state`() {
        vm.onSsidChanged("MyNetwork")
        assertEquals("MyNetwork", vm.uiState.value.ssid)
        assertNull(vm.uiState.value.ssidError)
    }

    @Test
    fun `onSsidChanged shows error for SSID exceeding 32 bytes`() {
        // 33 ASCII chars = 33 bytes > 32
        val longSsid = "A".repeat(33)
        vm.onSsidChanged(longSsid)
        assertNotNull(vm.uiState.value.ssidError)
    }

    @Test
    fun `onPasswordChanged updates state`() {
        vm.onPasswordChanged("securepass")
        assertEquals("securepass", vm.uiState.value.password)
        assertNull(vm.uiState.value.passwordError)
    }

    @Test
    fun `onPasswordChanged shows error for short password`() {
        vm.onPasswordChanged("short")
        assertNotNull(vm.uiState.value.passwordError)
    }

    @Test
    fun `onAccountNumberChanged updates state`() {
        vm.onAccountNumberChanged("ACC-123")
        assertEquals("ACC-123", vm.uiState.value.accountNumber)
        assertNull(vm.uiState.value.accountNumberError)
    }

    @Test
    fun `onSmsClicked with empty fields sets validation errors`() = runTest {
        val navigator = FakeNavigator()
        vm.onSmsClicked(navigator)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.customerNameError)
        assertNotNull(vm.uiState.value.customerPhoneError)
        assertNotNull(vm.uiState.value.ssidError)
        assertNotNull(vm.uiState.value.passwordError)
        assertNotNull(vm.uiState.value.accountNumberError)
        assertTrue(navigator.smsCalls.isEmpty())
    }

    @Test
    fun `onSmsClicked with invalid fields emits ValidationFailed`() = runTest {
        val navigator = FakeNavigator()

        vm.uiEvent.test {
            vm.onSmsClicked(navigator)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is UiEvent.ValidationFailed)
        }
    }

    @Test
    fun `onSmsClicked with valid fields calls navigator openSms`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.onSmsClicked(navigator)
        advanceUntilIdle()

        assertEquals(1, navigator.smsCalls.size)
        assertEquals("+12125551234", navigator.smsCalls[0].phoneNumber)
        assertTrue(navigator.smsCalls[0].message.contains("Alice"))
    }

    @Test
    fun `onShareClicked with valid fields calls navigator shareText`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.onShareClicked(navigator)
        advanceUntilIdle()

        assertEquals(1, navigator.shareCalls.size)
        assertTrue(navigator.shareCalls[0].message.contains("Alice"))
    }

    @Test
    fun `onCopyClicked with valid fields calls navigator copyToClipboard and emits events`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.uiEvent.test {
            vm.onCopyClicked(navigator)
            advanceUntilIdle()

            assertEquals(1, navigator.copyCalls.size)
            assertTrue(navigator.copyCalls[0].text.contains("Alice"))

            val event1 = awaitItem()
            assertTrue(event1 is UiEvent.CopySuccess)
            val event2 = awaitItem()
            assertTrue(event2 is UiEvent.ShowToast)
        }
    }

    @Test
    fun `onCopyClicked when clipboard fails emits ActionFailed and toast`() = runTest {
        val navigator = FakeNavigator().apply { clipboardSucceeds = false }
        fillValidFields()

        vm.uiEvent.test {
            vm.onCopyClicked(navigator)
            advanceUntilIdle()

            val event1 = awaitItem()
            assertTrue(event1 is UiEvent.ActionFailed)
            val event2 = awaitItem()
            assertTrue(event2 is UiEvent.ShowToast)
        }
    }

    @Test
    fun `rate limiting emits RateLimitExceeded on rapid actions`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.uiEvent.test {
            // First call succeeds - advance time to ensure cooldown passes
            fakeTimeProvider.advanceBy(3000L) // More than 2 second cooldown
            vm.onSmsClicked(navigator)
            advanceUntilIdle()

            // Immediate second call should be rate limited (no time advance)
            vm.onSmsClicked(navigator)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is UiEvent.RateLimitExceeded)
        }
    }

    @Test
    fun `clearForm resets all fields and shows toast`() = runTest {
        fillValidFields()
        assertTrue(vm.uiState.value.customerName.isNotBlank())

        vm.uiEvent.test {
            vm.clearForm()
            advanceUntilIdle()

            // Verify fields are cleared
            assertEquals("", vm.uiState.value.customerName)
            assertEquals("", vm.uiState.value.customerPhone)
            assertEquals("", vm.uiState.value.ssid)
            assertEquals("", vm.uiState.value.password)
            assertEquals("", vm.uiState.value.accountNumber)

            // Verify toast event is emitted (FakeResourceProvider returns "string_resId")
            val event = awaitItem()
            assertTrue(event is UiEvent.ShowToast)
            assertTrue((event as UiEvent.ShowToast).message.startsWith("string_"))
        }
    }

    @Test
    fun `message generation uses template and profile`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.onShareClicked(navigator)
        advanceUntilIdle()

        val message = navigator.shareCalls[0].message
        assertTrue(message.contains("Alice"))
        assertTrue(message.contains("TestWiFi"))
        assertTrue(message.contains("password123"))
        assertTrue(message.contains("ACC-001"))
    }

    @Test
    fun `onSmsClicked without phone number shows phone error`() = runTest {
        val navigator = FakeNavigator()
        vm.onCustomerNameChanged("Alice")
        vm.onSsidChanged("TestWiFi")
        vm.onPasswordChanged("password123")
        vm.onAccountNumberChanged("ACC-001")
        // No phone number set

        vm.onSmsClicked(navigator)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.customerPhoneError)
        assertTrue(navigator.smsCalls.isEmpty())
    }

    @Test
    fun `onShareClicked without phone number succeeds`() = runTest {
        val navigator = FakeNavigator()
        vm.onCustomerNameChanged("Alice")
        vm.onSsidChanged("TestWiFi")
        vm.onPasswordChanged("password123")
        vm.onAccountNumberChanged("ACC-001")
        // No phone — share doesn't require it

        vm.onShareClicked(navigator)
        advanceUntilIdle()

        assertEquals(1, navigator.shareCalls.size)
    }

    @Test
    fun `auto-clear clears form after timeout on resume`() = runTest {
        fillValidFields()
        assertTrue(vm.uiState.value.customerName.isNotBlank())

        vm.uiEvent.test {
            // Simulate going to background
            vm.onPause()

            // Advance time by more than 10 minutes
            fakeTimeProvider.advanceBy(11 * 60 * 1000L)

            // Resume - should auto-clear and show toast
            vm.onResume()
            advanceUntilIdle()

            // Verify fields are cleared
            assertEquals("", vm.uiState.value.customerName)
            assertEquals("", vm.uiState.value.customerPhone)

            // Verify toast event is emitted
            val event = awaitItem()
            assertTrue(event is UiEvent.ShowToast)
        }
    }

    @Test
    fun `auto-clear does not clear form if timeout not reached`() = runTest {
        fillValidFields()
        val originalName = vm.uiState.value.customerName

        // Simulate going to background
        vm.onPause()

        // Advance time by less than 10 minutes
        fakeTimeProvider.advanceBy(5 * 60 * 1000L)

        // Resume - should NOT clear
        vm.onResume()
        advanceUntilIdle()

        // Verify fields are NOT cleared
        assertEquals(originalName, vm.uiState.value.customerName)
    }

    @Test
    fun `auto-clear survives process death with SavedStateHandle`() = runTest {
        fillValidFields()

        // Simulate going to background
        vm.onPause()

        // Advance time
        fakeTimeProvider.advanceBy(11 * 60 * 1000L)

        // Simulate process death and recreation with same SavedStateHandle
        val newVm = CustomerIntakeViewModel(
            savedStateHandle = savedStateHandle, // Same SavedStateHandle
            settingsStore = mockStore,
            resourceProvider = fakeResourceProvider,
            timeProvider = fakeTimeProvider
        )

        newVm.uiEvent.test {
            // Resume on new instance - should auto-clear
            newVm.onResume()
            advanceUntilIdle()

            // Verify fields are cleared
            assertEquals("", newVm.uiState.value.customerName)

            // Verify toast event is emitted
            val event = awaitItem()
            assertTrue(event is UiEvent.ShowToast)
        }
    }

    private fun fillValidFields() {
        vm.onCustomerNameChanged("Alice")
        vm.onCustomerPhoneChanged("2125551234")
        vm.onSsidChanged("TestWiFi")
        vm.onPasswordChanged("password123")
        vm.onAccountNumberChanged("ACC-001")
    }
}


