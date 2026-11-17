package com.peercall.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun normalizesFormattedInternationalNumber() {
        assertEquals("+15551234567", PhoneNumberValidator.normalize("+1 (555) 123-4567"))
    }

    @Test
    fun acceptsSevenToFifteenDigits() {
        assertEquals("1234567", PhoneNumberValidator.normalize("123-4567"))
        assertEquals("123456789012345", PhoneNumberValidator.normalize("123456789012345"))
    }

    @Test
    fun rejectsMalformedOrOutOfRangeNumbers() {
        listOf("", "+++", "#", "123456", "1234567890123456", "12+34567", "12*34567")
            .forEach { assertNull(PhoneNumberValidator.normalize(it)) }
    }
}