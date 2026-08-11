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
import com.kingpaging.qwelcome.util.WifiQrGenerator
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
        every { mockStore.techProfileFlow } returns flowOf(TechProfile("Tech", "Sr Tech", "IT"))
        every { mockStore.activeTemplateFlow } returns flowOf(testTemplate)
        vm = CustomerIntakeViewModel(
            savedStateHandle = savedStateHandle,
            settingsStore = mockStore,
            resourceProvider = fakeResourceProvider,
            timeProvider = fakeTimeProvider,
            enableForegroundInactivityTimer = false
        )
    }

    @After
    fun tearDown() {
        vm.onPause()
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
    fun `security and hidden network options update state`() {
        vm.onSecurityTypeChanged(WifiQrGenerator.SecurityType.WPA3_SAE)
        vm.onHiddenNetworkChanged(true)

        assertEquals(WifiQrGenerator.SecurityType.WPA3_SAE, vm.uiState.value.securityType)
        assertTrue(vm.uiState.value.isHiddenNetwork)
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
    fun `clearFormWithUndo restores the manually cleared form`() {
        fillValidFields()

        val token = vm.clearFormWithUndo()
        assertEquals("", vm.uiState.value.customerName)

        vm.undoClearForm(token)

        assertEquals("Alice", vm.uiState.value.customerName)
        assertEquals("TestWiFi", vm.uiState.value.ssid)
    }

    @Test
    fun `discardClearFormUndo prevents restoring an expired clear`() {
        fillValidFields()

        val token = vm.clearFormWithUndo()
        vm.discardClearFormUndo(token)
        vm.undoClearForm(token)

        assertEquals("", vm.uiState.value.customerName)
    }

    @Test
    fun `discardClearFormUndo does not discard a newer snapshot`() {
        fillValidFields()
        val firstToken = vm.clearFormWithUndo()
        vm.onCustomerNameChanged("Bob")
        vm.onSsidChanged("SecondWiFi")
        val secondToken = vm.clearFormWithUndo()

        vm.discardClearFormUndo(firstToken)
        vm.undoClearForm(secondToken)

        assertEquals("Bob", vm.uiState.value.customerName)
        assertEquals("SecondWiFi", vm.uiState.value.ssid)
        assertTrue(firstToken != secondToken)
    }

    @Test
    fun `undoClearForm ignores a stale token and does not clobber the current snapshot`() {
        fillValidFields()
        val firstToken = vm.clearFormWithUndo()

        vm.onCustomerNameChanged("Bob")
        vm.onSsidChanged("SecondWiFi")
        val secondToken = vm.clearFormWithUndo()

        // A stale undo (e.g. from a first snackbar still visible when a second clear happened)
        // must not restore or consume the current snapshot.
        vm.undoClearForm(firstToken)
        assertEquals("", vm.uiState.value.customerName)

        // The current token can still restore its own snapshot.
        vm.undoClearForm(secondToken)
        assertEquals("Bob", vm.uiState.value.customerName)
        assertEquals("SecondWiFi", vm.uiState.value.ssid)
    }

    @Test
    fun `customer data preserves the entered name spelling`() {
        val name = "McDonald O'Neil"

        vm.onCustomerNameChanged(name)

        assertEquals(name, vm.uiState.value.toCustomerData().customerName)
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
    fun `onShareClicked only requires optional fields used by active template`() = runTest {
        every { mockStore.activeTemplateFlow } returns flowOf(
            testTemplate.copy(content = "Hello {{ customer_name }}, {{ ssid }}")
        )
        val navigator = FakeNavigator()
        vm.onCustomerNameChanged("Alice")
        vm.onSsidChanged("TestWiFi")

        vm.onShareClicked(navigator)
        advanceUntilIdle()

        assertEquals(1, navigator.shareCalls.size)
        assertNull(vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.accountNumberError)
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
            timeProvider = fakeTimeProvider,
            enableForegroundInactivityTimer = false
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

    @Test
    fun `auto-clear clears form after foreground inactivity timeout`() {
        fillValidFields()

        fakeTimeProvider.advanceBy(10 * 60 * 1000L)
        vm.checkInactivityTimeout()

        assertEquals("", vm.uiState.value.customerName)
        assertEquals("", vm.uiState.value.ssid)
    }

    @Test
    fun `auto-clear clears form from zero-valued activity timestamp`() = runTest {
        val zeroTimeProvider = FakeTimeProvider()
        val zeroTimestampState = SavedStateHandle()
        val zeroTimestampVm = CustomerIntakeViewModel(
            savedStateHandle = zeroTimestampState,
            settingsStore = mockStore,
            resourceProvider = fakeResourceProvider,
            timeProvider = zeroTimeProvider,
            enableForegroundInactivityTimer = false
        )

        zeroTimestampVm.onCustomerNameChanged("Alice")
        zeroTimeProvider.advanceBy(10 * 60 * 1000L)

        zeroTimestampVm.onResume()
        advanceUntilIdle()

        assertEquals("", zeroTimestampVm.uiState.value.customerName)
    }

    @Test
    fun `auto-clear clears form when saved timestamp is later than current elapsed time`() = runTest {
        // Simulates a device reboot: elapsedRealtime() resets to a small value while the
        // SavedStateHandle still holds a timestamp recorded before the reboot (larger value).
        val futureTimeProvider = FakeTimeProvider(1_000_000L)
        val rebootState = SavedStateHandle()
        val rebootVm = CustomerIntakeViewModel(
            savedStateHandle = rebootState,
            settingsStore = mockStore,
            resourceProvider = fakeResourceProvider,
            timeProvider = futureTimeProvider,
            enableForegroundInactivityTimer = false
        )

        rebootVm.onCustomerNameChanged("Alice")

        // elapsedRealtime() now reports a value lower than the saved timestamp.
        futureTimeProvider.setTime(500L)

        rebootVm.onResume()
        advanceUntilIdle()

        assertEquals("", rebootVm.uiState.value.customerName)
    }

    private fun fillValidFields() {
        vm.onCustomerNameChanged("Alice")
        vm.onCustomerPhoneChanged("2125551234")
        vm.onSsidChanged("TestWiFi")
        vm.onPasswordChanged("password123")
        vm.onAccountNumberChanged("ACC-001")
    }
}
