package com.peercall.esim

import org.junit.Assert.assertEquals
import org.junit.Test

class CarrierFailoverManagerTest {
    @Test
    fun selectsMostReliableSecondaryProfile() {
        val manager = CarrierFailoverManager()
        val profiles = listOf(
            EsimProfile("primary", "Work", "LocalCell", true, "Good signal", "Ready", true),
            EsimProfile("secondary", "Personal", "TravelLink", false, "Fair signal", "Standby"),
            EsimProfile("travel", "Travel", "RoamEasy", false, "Weak signal", "Ready for travel")
        )

        val chosen = manager.selectBestProfile(profiles, "primary")
        assertEquals("secondary", chosen)
    }

    @Test
    fun triggersFailoverWhenCurrentProfileIsWeak() {
        val manager = CarrierFailoverManager()
        val active = EsimProfile("primary", "Work", "LocalCell", true, "Weak signal", "Ready", true)
        assertEquals(true, manager.shouldFailover(1, active))
    }

    @Test
    fun picksEmergencyProfileFromBestSignal() {
        val manager = CarrierFailoverManager()
        val profiles = listOf(
            EsimProfile("primary", "Work", "LocalCell", false, "Weak signal", "Ready"),
            EsimProfile("secondary", "Personal", "TravelLink", false, "Good signal", "Standby")
        )

        val chosen = manager.emergencyProfile(profiles)
        assertEquals("secondary", chosen?.id)
    }
}
