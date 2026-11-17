package com.peercall.esim

import kotlin.math.max

data class CarrierSignalScore(
    val profileId: String,
    val signalBars: Int,
    val isActive: Boolean = false
)

class CarrierFailoverManager {
    fun selectBestProfile(profiles: List<EsimProfile>, currentProfileId: String?): String? {
        val candidates = profiles.filter { it.id != currentProfileId }
        if (candidates.isEmpty()) return currentProfileId

        val ranked = candidates.sortedByDescending { scoreProfile(it) }
        return ranked.firstOrNull()?.id ?: currentProfileId
    }

    fun shouldFailover(signalBars: Int, activeProfile: EsimProfile?): Boolean {
        if (activeProfile == null) return false
        return signalBars <= 1 && activeProfile.active
    }

    fun emergencyProfile(profiles: List<EsimProfile>): EsimProfile? =
        profiles.sortedByDescending { scoreProfile(it) }.firstOrNull()

    private fun scoreProfile(profile: EsimProfile): Int {
        val signalScore = when (profile.signalLabel.lowercase()) {
            "good signal" -> 80
            "fair signal" -> 60
            "weak signal" -> 30
            else -> 20
        }
        val activeBoost = if (profile.active) 20 else 0
        val statusBoost = when (profile.status.lowercase()) {
            "ready" -> 15
            "standby" -> 10
            "ready for travel" -> 8
            else -> 0
        }
        return max(0, signalScore + activeBoost + statusBoost)
    }
}
