package com.peercall.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureQuotaStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        "peercall_secure_quota",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveServerConfirmed(state: QuotaState) {
        prefs.edit()
            .putInt("minutes_remaining", state.minutesRemaining)
            .putInt("data_remaining_mb", state.dataRemainingMb)
            .putString("account_id", state.accountId)
            .putLong("confirmed_at_millis", state.confirmedAtMillis)
            .putBoolean("server_confirmed", true)
            .apply()
    }

    fun readServerConfirmed(): QuotaState? {
        if (!prefs.getBoolean("server_confirmed", false)) return null
        val accountId = prefs.getString("account_id", null) ?: return null
        val confirmedAtMillis = prefs.getLong("confirmed_at_millis", 0L)
        if (confirmedAtMillis <= 0L) return null

        return QuotaState(
            minutesRemaining = prefs.getInt("minutes_remaining", 0),
            dataRemainingMb = prefs.getInt("data_remaining_mb", 0),
            accountId = accountId,
            confirmedAtMillis = confirmedAtMillis
        )
    }
}
