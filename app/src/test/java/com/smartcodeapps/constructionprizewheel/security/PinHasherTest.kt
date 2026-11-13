package com.smartcodeapps.constructionprizewheel.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `correct pin matches`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.matches("1234", salt, hash))
    }

    @Test
    fun `wrong pin does not match`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertFalse(PinHasher.matches("1235", salt, hash))
    }

    @Test
    fun `hash does not contain the plain pin`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertNotEquals("1234", String(hash))
    }

    @Test
    fun `same pin with different salts gives different hashes`() {
        val first = PinHasher.hash("1234", PinHasher.newSalt())
        val second = PinHasher.hash("1234", PinHasher.newSalt())
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `same pin and salt is deterministic`() {
        val salt = PinHasher.newSalt()
        assertArrayEquals(PinHasher.hash("9876", salt), PinHasher.hash("9876", salt))
    }

    @Test
    fun `pin policy accepts exactly four digits`() {
        assertTrue(PinPolicy.isValid("1234"))
        assertTrue(PinPolicy.isValid("0000"))
        assertTrue(PinPolicy.isValid("0042"))
        assertFalse(PinPolicy.isValid("123"))
        assertFalse(PinPolicy.isValid("12345"))
        assertFalse(PinPolicy.isValid("123456"))
        assertFalse(PinPolicy.isValid("12a4"))
        assertFalse(PinPolicy.isValid(""))
    }
}
