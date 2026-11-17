package com.peercall.esim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EmergencyRoutingManagerTest {
    @Test
    fun selectsBestEmergencyProfileForWeakSignal() {
        val manager = EmergencyRoutingManager()
        val profiles = listOf(
            EsimProfile("primary", "Work", "LocalCell", true, "Weak signal", "Ready", true),
            EsimProfile("secondary", "Personal", "TravelLink", false, "Good signal", "Standby")
        )

        val chosen = manager.selectEmergencyProfile(profiles, 1)
        assertEquals("secondary", chosen?.id)
    }

    @Test
    fun usesStandardEmergencyNumber() {
        val manager = EmergencyRoutingManager()
        assertEquals("112", manager.emergencyNumber())
    }

    @Test
    fun triggersEmergencyFallbackWhenNoProfileIsUsable() {
        val manager = EmergencyRoutingManager()
        val profiles = listOf(
            EsimProfile("primary", "Work", "LocalCell", false, "Weak signal", "Ready")
        )

        assertEquals(true, manager.shouldUseEmergencyFallback(0, profiles))
    }
}
