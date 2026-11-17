package com.peercall.telephony

import org.junit.Assert.assertEquals
import org.junit.Test

class CallStateManagerTest {
    @Test
    fun resolvesActiveStateAboveDialingAndRinging() {
        val state = CallStateManager.resolvedStateFor(
            listOf(CallState.DISCONNECTED, CallState.DIALING, CallState.ACTIVE)
        )
        assertEquals(CallState.ACTIVE, state)
    }

    @Test
    fun ignoresDisconnectedInPriorityOrder() {
        val state = CallStateManager.resolvedStateFor(
            listOf(CallState.DISCONNECTED, CallState.IDLE)
        )
        assertEquals(CallState.IDLE, state)
    }

    @Test
    fun mapsDialingAndDisconnectedTelecomStates() {
        assertEquals(CallState.DIALING, CallStateManager.telecomStateToCallState(android.telecom.Call.STATE_DIALING))
        assertEquals(CallState.DISCONNECTED, CallStateManager.telecomStateToCallState(android.telecom.Call.STATE_DISCONNECTED))
    }
}
