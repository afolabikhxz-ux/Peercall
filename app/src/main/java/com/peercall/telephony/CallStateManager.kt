package com.peercall.telephony

import android.os.Build
import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallAudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED_HEADSET,
    UNKNOWN
}

object CallStateManager {
    private data class ManagedCall(
        val state: CallState,
        val destination: String?,
        val isOutgoing: Boolean,
        val audioRoute: CallAudioRoute = CallAudioRoute.EARPIECE
    )

    private val calls = mutableMapOf<Call, ManagedCall>()
    private val _state = MutableStateFlow(CallState.IDLE)
    private val _destination = MutableStateFlow<String?>(null)
    private val _audioRoute = MutableStateFlow(CallAudioRoute.UNKNOWN)
    private val _rescueDestination = MutableStateFlow<String?>(null)
    val state: StateFlow<CallState> = _state.asStateFlow()
    val destination: StateFlow<String?> = _destination.asStateFlow()
    val audioRoute: StateFlow<CallAudioRoute> = _audioRoute.asStateFlow()
    val rescueDestination: StateFlow<String?> = _rescueDestination.asStateFlow()
    private var pendingOutgoingDestination: String? = null
    private var rescueAlreadyTriggered = false

    fun beginOutgoingCall(destination: String) {
        pendingOutgoingDestination = destination
    }

    fun cancelPendingOutgoingCall() {
        pendingOutgoingDestination = null
    }

    fun onCallAdded(call: Call) {
        val outgoing = isOutgoing(call, call.state)
        val destination = if (outgoing) {
            pendingOutgoingDestination.also { pendingOutgoingDestination = null }
        } else {
            null
        }
        calls[call] = ManagedCall(mapState(call.state), destination, outgoing, _audioRoute.value)
        publishState()
    }

    fun onCallStateChanged(call: Call, telecomState: Int) {
        val previous = calls[call]
        val state = mapState(telecomState)
        val outgoing = previous?.isOutgoing == true || isOutgoing(call, telecomState)
        val destination = when {
            state == CallState.DISCONNECTED -> null
            previous?.destination != null -> previous.destination
            outgoing -> pendingOutgoingDestination.also { pendingOutgoingDestination = null }
            else -> null
        }
        calls[call] = ManagedCall(state, destination, outgoing, previous?.audioRoute ?: _audioRoute.value)

        if (state == CallState.DISCONNECTED && previous?.state == CallState.ACTIVE && previous.destination != null && !rescueAlreadyTriggered) {
            _rescueDestination.value = previous.destination
            rescueAlreadyTriggered = true
        } else if (state == CallState.ACTIVE) {
            rescueAlreadyTriggered = false
            _rescueDestination.value = null
        }

        if (state == CallState.DISCONNECTED && outgoing) cancelPendingOutgoingCall()
        publishState()
    }

    fun onCallAudioStateChanged(call: Call, audioState: Int) {
        val route = when (audioState) {
            CallAudioState.SPEAKER -> CallAudioRoute.SPEAKER
            CallAudioState.EARPIECE -> CallAudioRoute.EARPIECE
            CallAudioState.BLUETOOTH -> CallAudioRoute.BLUETOOTH
            CallAudioState.WIRED_HEADSET -> CallAudioRoute.WIRED_HEADSET
            else -> CallAudioRoute.UNKNOWN
        }
        val previous = calls[call]
        val updated = previous?.copy(audioRoute = route) ?: ManagedCall(CallState.IDLE, null, false, route)
        calls[call] = updated
        _audioRoute.value = route
    }

    fun onCallRemoved(call: Call) {
        val removed = calls.remove(call)
        if (removed?.isOutgoing == true) cancelPendingOutgoingCall()
        if (calls.isEmpty()) {
            rescueAlreadyTriggered = false
            _state.value = CallState.IDLE
            _destination.value = null
            _audioRoute.value = CallAudioRoute.UNKNOWN
            _rescueDestination.value = null
        } else {
            publishState()
        }
    }

    fun disconnectCurrentCall() {
        calls.entries.firstOrNull {
            it.value.state == CallState.ACTIVE ||
                it.value.state == CallState.DIALING ||
                it.value.state == CallState.RINGING
        }?.key?.disconnect()
    }

    fun answerCurrentRingingCall() {
        calls.entries.firstOrNull { it.value.state == CallState.RINGING }
            ?.key?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    internal fun resolvedStateFor(states: Collection<CallState>): CallState {
        return states.firstOrNull { it == CallState.ACTIVE }
            ?: states.firstOrNull { it == CallState.DIALING }
            ?: states.firstOrNull { it == CallState.RINGING }
            ?: states.firstOrNull { it == CallState.HOLDING }
            ?: CallState.IDLE
    }

    internal fun telecomStateToCallState(state: Int): CallState = mapState(state)

    private fun publishState() {
        val current = resolvedStateFor(calls.values.map { it.state })
        _state.value = current
        _destination.value = calls.values.firstOrNull { it.state == current }?.destination
    }

    fun clearRescueDestination() {
        _rescueDestination.value = null
        rescueAlreadyTriggered = false
    }

    private fun mapState(state: Int): CallState = when (state) {
        Call.STATE_RINGING -> CallState.RINGING
        Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
        Call.STATE_ACTIVE -> CallState.ACTIVE
        Call.STATE_HOLDING -> CallState.HOLDING
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.DISCONNECTED
        else -> CallState.IDLE
    }

    private fun isOutgoing(call: Call, state: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details.callDirection == Call.Details.DIRECTION_OUTGOING
        } else {
            state == Call.STATE_DIALING || state == Call.STATE_CONNECTING
        }
}

object CallAudioState {
    const val EARPIECE = 1
    const val SPEAKER = 2
    const val BLUETOOTH = 3
    const val WIRED_HEADSET = 4
}
