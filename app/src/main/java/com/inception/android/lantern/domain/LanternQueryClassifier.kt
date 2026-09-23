package com.inception.android.lantern.domain

import com.inception.android.lantern.model.LanternCategory
import java.util.Locale

/**
 * Rapid heuristic intent classifier for emergency survival queries.
 */
object LanternQueryClassifier {

    private val traumaKeywords = setOf(
        "cpr", "bleeding", "blood", "artery", "tourniquet", "heart attack", "unconscious",
        "pulse", "resuscitation", "cardiac", "chest compression", "hemorrhage", "cut"
    )

    private val firstAidKeywords = setOf(
        "choking", "heimlich", "burn", "blister", "scald", "heat", "frostbite",
        "hypothermia", "cold", "fracture", "bone", "sprain", "bandage", "wound", "airway"
    )

    private val waterKeywords = setOf(
        "water", "purify", "boil", "bleach", "disinfect", "filter", "sodis",
        "clean water", "drinking", "parasite", "giardia", "cholera", "turbid", "muddy"
    )

    private val disasterKeywords = setOf(
        "earthquake", "flood", "storm", "hurricane", "tornado", "aftershock",
        "tremor", "drown", "evacuate", "collapse", "tsunami", "landslide"
    )

    private val signalingKeywords = setOf(
        "signal", "sos", "whistle", "mirror", "rescue", "aircraft", "distress",
        "flare", "morse", "help", "sar"
    )

    fun classify(query: String): LanternCategory? {
        val normalized = query.lowercase(Locale.US)
        val tokens = normalized.split("[^a-z0-9]+".toRegex()).filter { it.isNotBlank() }

        if (tokens.any { traumaKeywords.contains(it) }) return LanternCategory.TRAUMA_CPR
        if (tokens.any { firstAidKeywords.contains(it) }) return LanternCategory.FIRST_AID
        if (tokens.any { waterKeywords.contains(it) }) return LanternCategory.WATER_SANITATION
        if (tokens.any { disasterKeywords.contains(it) }) return LanternCategory.DISASTER_PREP
        if (tokens.any { signalingKeywords.contains(it) }) return LanternCategory.SIGNALING

        return null
    }
}
