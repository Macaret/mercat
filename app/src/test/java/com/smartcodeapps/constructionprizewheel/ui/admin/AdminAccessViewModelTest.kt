package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.lifecycle.SavedStateHandle
import com.smartcodeapps.constructionprizewheel.security.AdminAuthentication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminAccessViewModelTest {

    private val auth = mockk<AdminAuthentication>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { auth.hasPin() } returns true
        coEvery { auth.verify(any()) } returns false
        coEvery { auth.verify("1234") } returns true
        coEvery { auth.setPin(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun AdminAccessViewModel.type(pin: String) = pin.forEach { onDigit(it.digitToInt()) }

    @Test
    fun `existing pin starts in login mode`() {
        assertEquals(AdminAccessMode.LOGIN, AdminAccessViewModel(auth, SavedStateHandle()).uiState.value.mode)
    }

    @Test
    fun `no pin starts in setup mode`() {
        coEvery { auth.hasPin() } returns false
        assertEquals(AdminAccessMode.SETUP_ENTER, AdminAccessViewModel(auth, SavedStateHandle()).uiState.value.mode)
    }

    @Test
    fun `digits are collected up to four and extra digits are ignored`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("12345678")
        assertEquals("1234", vm.uiState.value.pin)
    }

    @Test
    fun `zero can be entered like any other digit`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("0100")
        assertEquals("0100", vm.uiState.value.pin)
    }

    @Test
    fun `backspace removes only the most recently entered digit`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("120")
        vm.onBackspace()
        vm.onDigit(7)
        assertEquals("127", vm.uiState.value.pin)
    }

    @Test
    fun `backspace on an empty entry does nothing`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.onBackspace()
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun `backspace removes the last digit`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("123")
        vm.onBackspace()
        assertEquals("12", vm.uiState.value.pin)
        vm.onBackspace(); vm.onBackspace(); vm.onBackspace()
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun `submit is enabled only once four digits are entered`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        assertFalse(vm.uiState.value.canSubmit)
        vm.type("123")
        assertFalse(vm.uiState.value.canSubmit)
        vm.type("4")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `submitting a short pin does nothing`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("123")
        vm.onSubmit()

        assertEquals("123", vm.uiState.value.pin)
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.authenticated)
    }

    @Test
    fun `correct pin authenticates`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("1234")
        vm.onSubmit()
        assertTrue(vm.uiState.value.authenticated)
        assertEquals("", vm.uiState.value.pin)
    }

    @Test
    fun `wrong pin shows an error and clears the entry`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("9999")
        vm.onSubmit()

        val state = vm.uiState.value
        assertFalse(state.authenticated)
        assertEquals(AdminAccessError.INCORRECT_PIN, state.error)
        assertEquals("", state.pin)
    }

    @Test
    fun `failed attempts are not rate limited`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        repeat(20) {
            vm.type("9999")
            vm.onSubmit()
        }
        vm.type("1234")
        vm.onSubmit()
        assertTrue(vm.uiState.value.authenticated)
    }

    @Test
    fun `typing after an error clears the error`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("9999")
        vm.onSubmit()
        vm.onDigit(1)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `force setup starts in setup mode even when a pin already exists`() {
        val handle = SavedStateHandle(mapOf(FORCE_SETUP_ARG to true))
        assertEquals(AdminAccessMode.SETUP_ENTER, AdminAccessViewModel(auth, handle).uiState.value.mode)
    }

    @Test
    fun `changing the pin replaces it without asking for the old one`() {
        val vm = AdminAccessViewModel(auth, SavedStateHandle(mapOf(FORCE_SETUP_ARG to true)))
        vm.type("7351")
        vm.onSubmit()
        vm.type("7351")
        vm.onSubmit()

        coVerify(exactly = 1) { auth.setPin("7351") }
        coVerify(exactly = 0) { auth.verify(any()) }
        assertTrue(vm.uiState.value.authenticated)
    }

    @Test
    fun `without the force flag an existing pin means login mode`() {
        assertEquals(AdminAccessMode.LOGIN, AdminAccessViewModel(auth, SavedStateHandle(mapOf(FORCE_SETUP_ARG to false))).uiState.value.mode)
    }

    @Test
    fun `setup does not advance with a short pin`() {
        coEvery { auth.hasPin() } returns false
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("123")
        vm.onSubmit()

        assertEquals(AdminAccessMode.SETUP_ENTER, vm.uiState.value.mode)
        coVerify(exactly = 0) { auth.setPin(any()) }
    }

    @Test
    fun `setup stores the pin after a matching confirmation`() {
        coEvery { auth.hasPin() } returns false
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("4821")
        vm.onSubmit()
        assertEquals(AdminAccessMode.SETUP_CONFIRM, vm.uiState.value.mode)
        assertEquals("", vm.uiState.value.pin)

        vm.type("4821")
        vm.onSubmit()

        coVerify(exactly = 1) { auth.setPin("4821") }
        assertTrue(vm.uiState.value.authenticated)
    }

    @Test
    fun `setup mismatch returns to the first step without storing anything`() {
        coEvery { auth.hasPin() } returns false
        val vm = AdminAccessViewModel(auth, SavedStateHandle())
        vm.type("4821")
        vm.onSubmit()
        vm.type("4822")
        vm.onSubmit()

        assertEquals(AdminAccessMode.SETUP_ENTER, vm.uiState.value.mode)
        assertEquals(AdminAccessError.MISMATCH, vm.uiState.value.error)
        assertFalse(vm.uiState.value.authenticated)
        coVerify(exactly = 0) { auth.setPin(any()) }
    }
}
