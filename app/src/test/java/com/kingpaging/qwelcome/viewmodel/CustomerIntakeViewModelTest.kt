package com.kingpaging.qwelcome.viewmodel

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
    private lateinit var vm: CustomerIntakeViewModel

    private val testTemplate = Template(
        id = "test-template",
        name = "Test",
        content = "Hello {{ customer_name }}, SSID: {{ ssid }}, PW: {{ password }}, Acct: {{ account_number }}"
    )

    @Before
    fun setup() {
        every { mockStore.techProfileFlow } returns flowOf(TechProfile("Tech", "Sr Tech", "IT"))
        every { mockStore.activeTemplateFlow } returns flowOf(testTemplate)
        vm = CustomerIntakeViewModel(mockStore, fakeResourceProvider)
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
    fun `rate limiting emits RateLimitExceeded on rapid actions`() = runTest {
        val navigator = FakeNavigator()
        fillValidFields()

        vm.uiEvent.test {
            // First call succeeds
            vm.onSmsClicked(navigator)
            advanceUntilIdle()

            // Immediate second call should be rate limited
            vm.onSmsClicked(navigator)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is UiEvent.RateLimitExceeded)
        }
    }

    @Test
    fun `clearForm resets all fields`() {
        fillValidFields()
        assertNotNull(vm.uiState.value.customerName.isNotBlank())

        vm.clearForm()

        assertEquals("", vm.uiState.value.customerName)
        assertEquals("", vm.uiState.value.customerPhone)
        assertEquals("", vm.uiState.value.ssid)
        assertEquals("", vm.uiState.value.password)
        assertEquals("", vm.uiState.value.accountNumber)
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

    private fun fillValidFields() {
        vm.onCustomerNameChanged("Alice")
        vm.onCustomerPhoneChanged("2125551234")
        vm.onSsidChanged("TestWiFi")
        vm.onPasswordChanged("password123")
        vm.onAccountNumberChanged("ACC-001")
    }
}
