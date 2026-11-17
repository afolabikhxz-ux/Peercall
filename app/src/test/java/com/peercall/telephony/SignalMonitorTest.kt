package com.peercall.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SignalMonitorTest {
    @Test
    fun classifiesStrongSignalAsHealthy() {
        val snapshot = SignalQuality.classify(-72)
        assertEquals(4, snapshot.bars)
        assertEquals("Strong", snapshot.label)
        assertEquals(null, snapshot.warning)
    }

    @Test
    fun classifiesVeryWeakSignalAsWarning() {
        val snapshot = SignalQuality.classify(-115)
        assertEquals(0, snapshot.bars)
        assertEquals("No signal", snapshot.label)
        assertNotNull(snapshot.warning)
    }
}
