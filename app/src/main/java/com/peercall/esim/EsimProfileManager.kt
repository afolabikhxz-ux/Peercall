package com.peercall.esim

import android.content.Context

data class EsimProfile(
    val id: String,
    val name: String,
    val carrier: String,
    val active: Boolean,
    val signalLabel: String,
    val status: String,
    val isPrimary: Boolean = false
)

class EsimProfileManager(context: Context) {
    private val appContext = context.applicationContext

    fun profiles(): List<EsimProfile> {
        val activeCarrier = if (isLowMemoryDevice()) "Move" else "LocalCell"
        return listOf(
            EsimProfile(
                id = "primary",
                name = "Work",
                carrier = activeCarrier,
                active = true,
                signalLabel = "Good signal",
                status = "Ready",
                isPrimary = true
            ),
            EsimProfile(
                id = "secondary",
                name = "Personal",
                carrier = "TravelLink",
                active = false,
                signalLabel = "Fair signal",
                status = "Standby"
            ),
            EsimProfile(
                id = "travel",
                name = "Travel",
                carrier = "RoamEasy",
                active = false,
                signalLabel = "Weak signal",
                status = "Ready for travel"
            )
        )
    }

    fun activeProfile(): EsimProfile? = profiles().firstOrNull { it.active }

    fun switchToProfile(id: String): EsimProfile? {
        val current = profiles().map { it.copy(active = it.id == id) }
        return current.firstOrNull { it.id == id }
    }

    private fun isLowMemoryDevice(): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory() / (1024 * 1024)
        return maxMemoryMb < 1024
    }
}
