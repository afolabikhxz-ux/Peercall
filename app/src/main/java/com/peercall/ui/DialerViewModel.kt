package com.peercall.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peercall.data.QuotaRepository
import com.peercall.esim.EmergencyRoutingManager
import com.peercall.esim.EsimProfileManager
import com.peercall.esim.EsimProfile
import com.peercall.telephony.CallLauncher
import com.peercall.telephony.CallState
import com.peercall.telephony.CallStateManager
import com.peercall.telephony.LockScreenFallbackManager
import com.peercall.telephony.PhoneNumberValidator
import com.peercall.telephony.SignalMonitor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class DialerUiState(
    val number: String = "",
    val accountId: String = "Unavailable",
    val minutesRemaining: Int? = null,
    val status: String = "Balance not yet confirmed",
    val callState: CallState = CallState.IDLE,
    val callDestination: String? = null,
    val isStartingCall: Boolean = false,
    val signalBars: Int = 0,
    val signalLabel: String = "No signal",
    val signalWarning: String? = null,
    val qualityScore: Int = 0,
    val profiles: List<EsimProfile> = emptyList(),
    val emergencyNotice: String? = null,
    val lockScreenFallbackNotice: String? = null
)

class DialerViewModel(
    private val repository: QuotaRepository,
    private val callLauncher: CallLauncher,
    private val signalMonitor: SignalMonitor,
    private val profileManager: EsimProfileManager
) : ViewModel() {
    private val emergencyRoutingManager = EmergencyRoutingManager()
    private val lockScreenFallbackManager = LockScreenFallbackManager()

    private val cachedQuota = repository.getQuota()
    private val _uiState = MutableStateFlow(
        DialerUiState(
            accountId = cachedQuota?.accountId ?: "Unavailable",
            minutesRemaining = cachedQuota?.minutesRemaining,
            profiles = profileManager.profiles(),
            status = if (cachedQuota == null) {
                "Balance not yet confirmed"
            } else {
                "Showing last server-confirmed balance"
            }
        )
    )
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    init {
        refreshSignalState()

        viewModelScope.launch {
            var previousState = CallStateManager.state.value
            var previousDestination = CallStateManager.destination.value
            combine(CallStateManager.state, CallStateManager.destination, CallStateManager.rescueDestination) { state, destination, rescue ->
                Triple(state, destination, rescue)
            }.collect { (state, destination, rescue) ->
                val status = when (state) {
                    CallState.RINGING -> "Incoming call"
                    CallState.DIALING -> "Calling..."
                    CallState.ACTIVE -> "Call connected"
                    CallState.HOLDING -> "Call on hold"
                    CallState.DISCONNECTED -> if (previousState.isInProgress()) {
                        if (previousDestination != null) {
                            "Call ended. Usage confirmation pending."
                        } else {
                            "Call ended"
                        }
                    } else {
                        null
                    }
                    CallState.IDLE -> null
                }

                val updatedStatus = when {
                    rescue != null -> "Call rescue scheduled for $rescue"
                    status != null -> status
                    else -> _uiState.value.status
                }

                _uiState.value = _uiState.value.copy(
                    callState = state,
                    callDestination = destination,
                    isStartingCall = when (state) {
                        CallState.DIALING -> _uiState.value.isStartingCall
                        CallState.RINGING,
                        CallState.ACTIVE,
                        CallState.HOLDING,
                        CallState.DISCONNECTED -> false
                        CallState.IDLE -> _uiState.value.isStartingCall
                    },
                    status = updatedStatus
                )

                if (rescue != null) {
                    viewModelScope.launch {
                        delay(2_000)
                        callLauncher.placeCall(rescue)
                        CallStateManager.clearRescueDestination()
                    }
                }

                previousState = state
                previousDestination = destination
            }
        }
    }

    fun onNumberChanged(value: String) {
        _uiState.value = _uiState.value.copy(number = value)
    }

    fun triggerEmergencyFallback() {
        val profiles = _uiState.value.profiles
        val fallbackProfile = emergencyRoutingManager.selectEmergencyProfile(profiles, _uiState.value.signalBars)
        val notice = if (fallbackProfile != null) {
            "Emergency route: ${fallbackProfile.carrier} • ${emergencyRoutingManager.emergencyNumber()}"
        } else {
            "Emergency route: ${emergencyRoutingManager.emergencyNumber()}"
        }
        _uiState.value = _uiState.value.copy(emergencyNotice = notice)
    }

    fun setLockScreenFallback(blocked: Boolean) {
        val notice = if (lockScreenFallbackManager.shouldUseNotificationFallback(blocked)) {
            lockScreenFallbackManager.fallbackReason()
        } else null
        _uiState.value = _uiState.value.copy(lockScreenFallbackNotice = notice)
    }

    fun refreshSignalState() {
        val snapshot = signalMonitor.current()
        _uiState.value = _uiState.value.copy(
            signalBars = snapshot.bars,
            signalLabel = snapshot.label,
            signalWarning = snapshot.warning,
            qualityScore = when {
                snapshot.isWeak -> 25
                snapshot.bars >= 3 -> 80
                snapshot.bars >= 2 -> 60
                else -> 45
            }
        )
    }

    fun onCallClicked() {
        val currentState = _uiState.value
        if (currentState.isStartingCall || currentState.callState.isInProgress()) return

        val number = PhoneNumberValidator.normalize(currentState.number)
        if (number == null) {
            _uiState.value = currentState.copy(status = "Enter a valid phone number")
            return
        }

        _uiState.value = currentState.copy(isStartingCall = true, status = "Placing call...")
        CallStateManager.beginOutgoingCall(number)
        viewModelScope.launch {
            try {
                callLauncher.placeCall(number)
                withTimeout(CALL_SETUP_TIMEOUT_MS) {
                    CallStateManager.state.first { it.isInProgress() }
                }
            } catch (_: TimeoutCancellationException) {
                CallStateManager.cancelPendingOutgoingCall()
                _uiState.value = _uiState.value.copy(
                    isStartingCall = false,
                    status = "Call was not detected. Check dialer setup."
                )
            } catch (_: SecurityException) {
                CallStateManager.cancelPendingOutgoingCall()
                _uiState.value = _uiState.value.copy(
                    isStartingCall = false,
                    status = "Phone permission is required to place a call"
                )
            } catch (_: Exception) {
                CallStateManager.cancelPendingOutgoingCall()
                _uiState.value = _uiState.value.copy(
                    isStartingCall = false,
                    status = "Unable to place call"
                )
            }
        }
    }

    fun onCallSetupUnavailable(message: String) {
        _uiState.value = _uiState.value.copy(isStartingCall = false, status = message)
    }

    private fun CallState.isInProgress(): Boolean = when (this) {
        CallState.RINGING, CallState.DIALING, CallState.ACTIVE, CallState.HOLDING -> true
        CallState.IDLE, CallState.DISCONNECTED -> false
    }

    companion object {
        private const val CALL_SETUP_TIMEOUT_MS = 20_000L
    }
}
