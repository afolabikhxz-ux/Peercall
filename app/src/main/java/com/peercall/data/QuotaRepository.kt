package com.peercall.data

import android.content.Context

data class QuotaState(
    val minutesRemaining: Int,
    val dataRemainingMb: Int,
    val accountId: String,
    val confirmedAtMillis: Long
)

class QuotaRepository(context: Context) {
    private val secureStore = SecureQuotaStore(context)

    fun getQuota(): QuotaState? = secureStore.readServerConfirmed()
}
