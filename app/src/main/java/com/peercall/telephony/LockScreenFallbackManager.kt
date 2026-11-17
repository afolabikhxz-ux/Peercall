package com.peercall.telephony

class LockScreenFallbackManager {
    fun shouldUseNotificationFallback(fullScreenBlocked: Boolean): Boolean = fullScreenBlocked

    fun fallbackReason(): String = "Use the notification action to answer or reject the call."

    fun answerActionLabel(): String = "Answer"

    fun rejectActionLabel(): String = "Reject"
}
