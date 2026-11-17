package com.peercall.esim

class EmergencyRoutingManager {
    fun selectEmergencyProfile(profiles: List<EsimProfile>, signalBars: Int): EsimProfile? {
        if (profiles.isEmpty()) return null
        val eligible = profiles.filter { it.status != "inactive" }
        if (eligible.isEmpty()) return profiles.first()
        return eligible.sortedByDescending { score(it, signalBars) }.first()
    }

    fun emergencyNumber(): String = "112"

    fun shouldUseEmergencyFallback(signalBars: Int, profiles: List<EsimProfile>): Boolean =
        signalBars <= 1 || profiles.none { it.active }

    private fun score(profile: EsimProfile, signalBars: Int): Int {
        val signalWeight = when (profile.signalLabel.lowercase()) {
            "good signal" -> 50
            "fair signal" -> 35
            "weak signal" -> 20
            else -> 10
        }
        val activeWeight = if (profile.active) 25 else 0
        val strengthWeight = signalBars * 10
        return signalWeight + activeWeight + strengthWeight
    }
}
