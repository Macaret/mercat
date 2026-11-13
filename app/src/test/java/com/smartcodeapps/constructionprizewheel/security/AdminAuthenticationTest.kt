package com.smartcodeapps.constructionprizewheel.security

import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAuthenticationTest {

    private val settings = mockk<SettingsRepository>(relaxUnitFun = true)
    private val auth = AdminAuthentication(settings)

    @Test
    fun `hasPin reflects whether a pin is stored`() = runTest {
        every { settings.hasPin } returns flowOf(false)
        assertFalse(auth.hasPin())

        every { settings.hasPin } returns flowOf(true)
        assertTrue(auth.hasPin())
    }

    @Test
    fun `verify delegates to the stored hash check`() = runTest {
        coEvery { settings.verifyPin("1234") } returns true
        coEvery { settings.verifyPin("0000") } returns false

        assertTrue(auth.verify("1234"))
        assertFalse(auth.verify("0000"))
    }

    @Test
    fun `invalid pins are rejected and never stored`() = runTest {
        listOf("", "123", "12345", "123456", "12ab", "12 34").forEach { pin ->
            assertFalse("pin '$pin'", auth.setPin(pin))
        }
        coVerify(exactly = 0) { settings.setPin(any()) }
    }

    @Test
    fun `four digit pins including zeros are stored`() = runTest {
        assertTrue(auth.setPin("1234"))
        assertTrue(auth.setPin("0000"))
        coVerify(exactly = 1) { settings.setPin("1234") }
        coVerify(exactly = 1) { settings.setPin("0000") }
    }
}
