package com.peercall.telephony

import android.content.Context
import android.net.Uri
import android.telecom.TelecomManager

interface CallLauncher {
    fun placeCall(number: String)
}

class TelecomCallLauncher(
    context: Context
) : CallLauncher {
    private val telecomManager = context.getSystemService(TelecomManager::class.java)

    override fun placeCall(number: String) {
        val manager = telecomManager ?: throw IllegalStateException("TelecomManager not available")
        manager.placeCall(Uri.fromParts("tel", number, null), null)
    }
}
