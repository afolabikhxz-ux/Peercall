package com.peercall.telephony

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class PeercallInCallService : InCallService() {
    private val callListeners = mutableMapOf<Call, Call.Callback>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val listener = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                CallStateManager.onCallStateChanged(call, state)
                Log.d(TAG, "Call state changed: ${CallStateManager.state.value}")
            }

            override fun onAudioStateChanged(call: Call, audioState: Int) {
                super.onAudioStateChanged(call, audioState)
                CallStateManager.onCallAudioStateChanged(call, audioState)
                Log.d(TAG, "Call audio state changed: $audioState")
            }
        }
        CallStateManager.onCallAdded(call)
        callListeners[call] = listener
        call.registerCallback(listener)
        startActivity(
            Intent(this, com.peercall.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        Log.d(TAG, "Call added: ${call.details.handle}")
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        callListeners.remove(call)?.let(call::unregisterCallback)
        CallStateManager.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details.handle}")
    }

    override fun onDestroy() {
        super.onDestroy()
        callListeners.forEach { (call, callback) -> call.unregisterCallback(callback) }
        callListeners.clear()
        CallStateManager.cancelPendingOutgoingCall()
    }

    companion object {
        private const val TAG = "PeercallInCallService"
    }
}
