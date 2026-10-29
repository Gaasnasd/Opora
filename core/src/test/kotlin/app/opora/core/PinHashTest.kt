package app.opora.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinHashTest {
    @Test
    fun verifiesOnlyTheRightPin() {
        val data = PinHash.create("4821")
        assertTrue(PinHash.verify("4821", data))
        assertFalse(PinHash.verify("4822", data))
        assertFalse(PinHash.verify("", data))
    }

    @Test
    fun saltDiffersEachTime() {
        assertNotEquals(PinHash.create("4821").salt, PinHash.create("4821").salt)
    }

    @Test
    fun pinIsFourToEightDigits() {
        assertFalse(PinHash.isValid("123"))
        assertTrue(PinHash.isValid("1234"))
        assertTrue(PinHash.isValid("12345678"))
        assertFalse(PinHash.isValid("123456789"))
        assertFalse(PinHash.isValid("12a4"))
    }
}
