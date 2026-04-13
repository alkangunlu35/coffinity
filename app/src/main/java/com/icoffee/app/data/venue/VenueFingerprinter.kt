package com.icoffee.app.data.venue

import com.icoffee.app.data.model.DetectedMenuItem
import com.icoffee.app.data.model.NormalizedCoffeeType
import java.security.MessageDigest

object VenueFingerprinter {

    /** Sorted, comma-separated type names — e.g. "AMERICANO,CAPPUCCINO,ESPRESSO,LATTE" */
    fun signature(detectedItems: List<DetectedMenuItem>): String =
        detectedItems
            .map { it.normalizedType }
            .filter { it != NormalizedCoffeeType.UNKNOWN }
            .map { it.name }
            .toSortedSet()
            .joinToString(",")

    /** SHA-256 of the signature string */
    fun fingerprint(detectedItems: List<DetectedMenuItem>): String =
        sha256(signature(detectedItems))

    fun fingerprintFromSignature(signature: String): String = sha256(signature)

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
