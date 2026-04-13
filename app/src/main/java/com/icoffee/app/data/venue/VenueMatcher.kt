package com.icoffee.app.data.venue

import com.icoffee.app.data.db.VenueEntity
import com.icoffee.app.data.db.toVenue
import com.icoffee.app.data.model.VenueMatchResult

object VenueMatcher {

    private const val PARTIAL_MATCH_THRESHOLD = 0.70

    /**
     * Returns the best venue match from [candidates].
     * Exact fingerprint → confidence 100.
     * Jaccard similarity ≥ 0.70 on signature sets → partial match with proportional confidence.
     * Otherwise → isNew = true, venue = null.
     */
    fun match(
        fingerprint: String,
        signature: String,
        candidates: List<VenueEntity>
    ): VenueMatchResult {
        if (candidates.isEmpty() || signature.isBlank()) {
            return VenueMatchResult(venue = null, isNew = true, confidence = 0)
        }

        val exact = candidates.firstOrNull { it.menuFingerprint == fingerprint }
        if (exact != null) {
            return VenueMatchResult(venue = exact.toVenue(), isNew = false, confidence = 100)
        }

        val sigSet = signature.split(",").filter { it.isNotBlank() }.toSet()
        val (bestCandidate, bestJaccard) = candidates
            .map { candidate ->
                val candidateSet = candidate.menuSignature.split(",").filter { it.isNotBlank() }.toSet()
                val intersection = sigSet.intersect(candidateSet).size.toDouble()
                val union = sigSet.union(candidateSet).size.toDouble().coerceAtLeast(1.0)
                candidate to (intersection / union)
            }
            .maxByOrNull { it.second }
            ?: return VenueMatchResult(venue = null, isNew = true, confidence = 0)

        return if (bestJaccard >= PARTIAL_MATCH_THRESHOLD) {
            VenueMatchResult(
                venue = bestCandidate.toVenue(),
                isNew = false,
                confidence = (bestJaccard * 100).toInt()
            )
        } else {
            VenueMatchResult(venue = null, isNew = true, confidence = 0)
        }
    }
}
