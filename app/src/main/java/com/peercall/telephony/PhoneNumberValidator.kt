package com.peercall.telephony

object PhoneNumberValidator {
    fun normalize(input: String): String? {
        val value = input.trim()
        if (value.isEmpty() || value.any { it !in '0'..'9' && it !in " +()- ." }) return null
        if (value.count { it == '+' } > 1 || ('+' in value && value.first() != '+')) return null

        val digits = value.filter { it in '0'..'9' }
        if (digits.length !in 7..15) return null

        return (if (value.startsWith('+')) "+" else "") + digits
    }
}