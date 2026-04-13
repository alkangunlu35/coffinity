package com.icoffee.app.data.firebase.model

import com.google.firebase.firestore.DocumentSnapshot

private const val FIELD_ID = "id"

private fun DocumentSnapshot.requiredId(): String? {
    val explicit = getString(FIELD_ID).orEmpty().trim()
    val resolved = if (explicit.isNotBlank()) explicit else id.trim()
    return resolved.ifBlank { null }
}

private fun DocumentSnapshot.string(field: String): String =
    getString(field).orEmpty()

private fun DocumentSnapshot.optionalString(field: String): String? =
    getString(field)?.trim()?.takeIf { it.isNotBlank() }

private fun DocumentSnapshot.bool(field: String, fallback: Boolean = false): Boolean =
    getBoolean(field) ?: fallback

private fun DocumentSnapshot.long(field: String, fallback: Long = 0L): Long =
    getLong(field) ?: fallback

private fun DocumentSnapshot.int(field: String, fallback: Int = 0): Int =
    (getLong(field)?.toInt()) ?: fallback

private fun DocumentSnapshot.double(field: String, fallback: Double = 0.0): Double {
    val value = get(field) as? Number ?: return fallback
    return value.toDouble()
}

private fun DocumentSnapshot.stringList(field: String): List<String> {
    val raw = get(field) as? List<*> ?: return emptyList()
    return raw.mapNotNull { it as? String }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.map(field: String): Map<String, Any?> {
    return get(field) as? Map<String, Any?> ?: emptyMap()
}

data class FirestoreUser(
    val id: String,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val city: String = "",
    val country: String = "",
    val language: String = "en",
    val plan: String = "free",
    val role: String = "user",
    val managedBrandIds: List<String> = emptyList(),
    val discoverable: Boolean = true,
    val profileCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "displayName" to displayName,
        "email" to email,
        "photoUrl" to photoUrl,
        "city" to city,
        "country" to country,
        "language" to language,
        "plan" to plan,
        "role" to role,
        "managedBrandIds" to managedBrandIds,
        "discoverable" to discoverable,
        "profileCompleted" to profileCompleted,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreUser(): FirestoreUser? {
    val id = requiredId() ?: return null
    return FirestoreUser(
        id = id,
        displayName = string("displayName"),
        email = string("email"),
        photoUrl = optionalString("photoUrl"),
        city = string("city"),
        country = string("country"),
        language = string("language").ifBlank { "en" },
        plan = string("plan").ifBlank { "free" },
        role = string("role").ifBlank { "user" },
        managedBrandIds = stringList("managedBrandIds")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
        discoverable = bool("discoverable", fallback = true),
        profileCompleted = bool("profileCompleted", fallback = false),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreBrand(
    val id: String,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val country: String = "",
    val city: String = "",
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val website: String? = null,
    val instagram: String? = null,
    val sourceUrl: String? = null,
    val category: String = "",
    val status: String = "draft",
    val source: String? = null,
    val sourceSuggestionId: String? = null,
    val mergedSuggestionIds: List<String> = emptyList(),
    val ownerUserId: String? = null,
    val ownerEmail: String? = null,
    val managedByUserIds: List<String> = emptyList(),
    val verified: Boolean = false,
    val featured: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val deletedByUserId: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val productCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    private fun isActiveStatus(): Boolean {
        return status.trim().lowercase() in setOf("active", "claimed", "business")
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "slug" to slug,
        "description" to description,
        "country" to country,
        "city" to city,
        "logoUrl" to logoUrl,
        "coverImageUrl" to coverImageUrl,
        "website" to website,
        "instagram" to instagram,
        "sourceUrl" to sourceUrl,
        "category" to category,
        "status" to status,
        "isActive" to isActiveStatus(),
        "source" to source,
        "sourceSuggestionId" to sourceSuggestionId,
        "mergedSuggestionIds" to mergedSuggestionIds,
        "ownerUserId" to ownerUserId,
        "ownerEmail" to ownerEmail,
        "managedByUserIds" to managedByUserIds,
        "verified" to verified,
        "featured" to featured,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "deletedByUserId" to deletedByUserId,
        "averageRating" to averageRating,
        "reviewCount" to reviewCount,
        "productCount" to productCount,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreBrand(): FirestoreBrand? {
    val id = requiredId() ?: return null
    val resolvedStatus = string("status")
        .ifBlank {
            if (bool("isActive", fallback = false)) "active" else "draft"
        }
    return FirestoreBrand(
        id = id,
        name = string("name"),
        slug = string("slug"),
        description = string("description"),
        country = string("country"),
        city = string("city"),
        logoUrl = optionalString("logoUrl"),
        coverImageUrl = optionalString("coverImageUrl"),
        website = optionalString("website"),
        instagram = optionalString("instagram"),
        sourceUrl = optionalString("sourceUrl"),
        category = string("category"),
        status = resolvedStatus,
        source = optionalString("source"),
        sourceSuggestionId = optionalString("sourceSuggestionId"),
        mergedSuggestionIds = stringList("mergedSuggestionIds")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
        ownerUserId = optionalString("ownerUserId"),
        ownerEmail = optionalString("ownerEmail"),
        managedByUserIds = stringList("managedByUserIds")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
        verified = bool("verified"),
        featured = bool("featured"),
        isDeleted = bool("isDeleted"),
        deletedAt = getLong("deletedAt"),
        deletedByUserId = optionalString("deletedByUserId"),
        averageRating = double("averageRating"),
        reviewCount = int("reviewCount"),
        productCount = int("productCount"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreBrandSuggestion(
    val id: String,
    val submittedByUserId: String,
    val submittedByDisplayName: String = "",
    val submittedByEmail: String = "",
    val brandName: String,
    val normalizedBrandName: String,
    val slugCandidate: String = "",
    val websiteUrl: String? = null,
    val instagramUrl: String? = null,
    val country: String? = null,
    val city: String? = null,
    val description: String? = null,
    val sourceType: String = "manual_user_suggestion",
    val status: String = "pending",
    val duplicateCandidateBrandIds: List<String> = emptyList(),
    val duplicateCheckVersion: Int = 1,
    val adminNotes: String? = null,
    val rejectionReason: String? = null,
    val resolvedByUserId: String? = null,
    val resolvedAt: Long? = null,
    val createdBrandId: String? = null,
    val mergedIntoBrandId: String? = null,
    val lastActionType: String? = null,
    val flagsPossibleDuplicate: Boolean = false,
    val flagsHasWebsite: Boolean = false,
    val flagsHasInstagram: Boolean = false,
    val flagsLowQualityText: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "submittedByUserId" to submittedByUserId,
        "submittedByDisplayName" to submittedByDisplayName,
        "submittedByEmail" to submittedByEmail,
        "brandName" to brandName,
        "normalizedBrandName" to normalizedBrandName,
        "slugCandidate" to slugCandidate,
        "websiteUrl" to websiteUrl,
        "instagramUrl" to instagramUrl,
        "country" to country,
        "city" to city,
        "description" to description,
        "sourceType" to sourceType,
        "status" to status,
        "duplicateCandidateBrandIds" to duplicateCandidateBrandIds,
        "duplicateCheckVersion" to duplicateCheckVersion,
        "adminNotes" to adminNotes,
        "rejectionReason" to rejectionReason,
        "resolvedByUserId" to resolvedByUserId,
        "resolvedAt" to resolvedAt,
        "createdBrandId" to createdBrandId,
        "mergedIntoBrandId" to mergedIntoBrandId,
        "lastActionType" to lastActionType,
        "flags" to mapOf(
            "possibleDuplicate" to flagsPossibleDuplicate,
            "hasWebsite" to flagsHasWebsite,
            "hasInstagram" to flagsHasInstagram,
            "lowQualityText" to flagsLowQualityText
        ),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreBrandSuggestion(): FirestoreBrandSuggestion? {
    val id = requiredId() ?: return null
    val submittedByUserId = string("submittedByUserId")
    val brandName = string("brandName")
    if (submittedByUserId.isBlank() || brandName.isBlank()) return null
    return FirestoreBrandSuggestion(
        id = id,
        submittedByUserId = submittedByUserId,
        submittedByDisplayName = string("submittedByDisplayName"),
        submittedByEmail = string("submittedByEmail"),
        brandName = brandName,
        normalizedBrandName = string("normalizedBrandName").ifBlank { brandName.trim().lowercase() },
        slugCandidate = string("slugCandidate"),
        websiteUrl = optionalString("websiteUrl"),
        instagramUrl = optionalString("instagramUrl"),
        country = optionalString("country"),
        city = optionalString("city"),
        description = optionalString("description"),
        sourceType = string("sourceType").ifBlank { "manual_user_suggestion" },
        status = string("status").ifBlank { "pending" },
        duplicateCandidateBrandIds = stringList("duplicateCandidateBrandIds")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
        duplicateCheckVersion = int("duplicateCheckVersion", fallback = 1),
        adminNotes = optionalString("adminNotes"),
        rejectionReason = optionalString("rejectionReason"),
        resolvedByUserId = optionalString("resolvedByUserId"),
        resolvedAt = getLong("resolvedAt"),
        createdBrandId = optionalString("createdBrandId"),
        mergedIntoBrandId = optionalString("mergedIntoBrandId"),
        lastActionType = optionalString("lastActionType"),
        flagsPossibleDuplicate = (map("flags")["possibleDuplicate"] as? Boolean) ?: false,
        flagsHasWebsite = (map("flags")["hasWebsite"] as? Boolean) ?: false,
        flagsHasInstagram = (map("flags")["hasInstagram"] as? Boolean) ?: false,
        flagsLowQualityText = (map("flags")["lowQualityText"] as? Boolean) ?: false,
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreSuggestionActionLog(
    val id: String,
    val suggestionId: String,
    val actionType: String,
    val actorUserId: String,
    val actorRole: String,
    val createdAt: Long,
    val previousStatus: String? = null,
    val nextStatus: String? = null,
    val notes: String? = null,
    val rejectionReason: String? = null,
    val createdBrandId: String? = null,
    val mergedIntoBrandId: String? = null,
    val snapshotBrandName: String? = null,
    val snapshotNormalizedBrandName: String? = null,
    val snapshotWebsiteUrl: String? = null,
    val snapshotCountry: String? = null,
    val snapshotCity: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "suggestionId" to suggestionId,
        "actionType" to actionType,
        "actorUserId" to actorUserId,
        "actorRole" to actorRole,
        "createdAt" to createdAt,
        "previousStatus" to previousStatus,
        "nextStatus" to nextStatus,
        "notes" to notes,
        "rejectionReason" to rejectionReason,
        "createdBrandId" to createdBrandId,
        "mergedIntoBrandId" to mergedIntoBrandId,
        "snapshot" to mapOf(
            "brandName" to snapshotBrandName,
            "normalizedBrandName" to snapshotNormalizedBrandName,
            "websiteUrl" to snapshotWebsiteUrl,
            "country" to snapshotCountry,
            "city" to snapshotCity
        )
    )
}

fun DocumentSnapshot.toFirestoreSuggestionActionLog(): FirestoreSuggestionActionLog? {
    val id = requiredId() ?: return null
    val suggestionId = string("suggestionId")
    val actionType = string("actionType")
    val actorUserId = string("actorUserId")
    if (suggestionId.isBlank() || actionType.isBlank() || actorUserId.isBlank()) return null
    val snapshot = map("snapshot")
    return FirestoreSuggestionActionLog(
        id = id,
        suggestionId = suggestionId,
        actionType = actionType,
        actorUserId = actorUserId,
        actorRole = string("actorRole"),
        createdAt = long("createdAt"),
        previousStatus = optionalString("previousStatus"),
        nextStatus = optionalString("nextStatus"),
        notes = optionalString("notes"),
        rejectionReason = optionalString("rejectionReason"),
        createdBrandId = optionalString("createdBrandId"),
        mergedIntoBrandId = optionalString("mergedIntoBrandId"),
        snapshotBrandName = (snapshot["brandName"] as? String)?.trim()?.ifBlank { null },
        snapshotNormalizedBrandName = (snapshot["normalizedBrandName"] as? String)?.trim()?.ifBlank { null },
        snapshotWebsiteUrl = (snapshot["websiteUrl"] as? String)?.trim()?.ifBlank { null },
        snapshotCountry = (snapshot["country"] as? String)?.trim()?.ifBlank { null },
        snapshotCity = (snapshot["city"] as? String)?.trim()?.ifBlank { null }
    )
}

data class FirestoreProduct(
    val id: String,
    val brandId: String,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val barcode: String? = null,
    val origin: String = "",
    val roastLevel: String = "",
    val process: String = "",
    val tastingNotes: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val importedVia: String? = null,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "brandId" to brandId,
        "name" to name,
        "slug" to slug,
        "description" to description,
        "imageUrl" to imageUrl,
        "barcode" to barcode,
        "origin" to origin,
        "roastLevel" to roastLevel,
        "process" to process,
        "tastingNotes" to tastingNotes,
        "sourceUrl" to sourceUrl,
        "importedVia" to importedVia,
        "averageRating" to averageRating,
        "reviewCount" to reviewCount,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreProduct(): FirestoreProduct? {
    val id = requiredId() ?: return null
    val brandId = string("brandId")
    if (brandId.isBlank()) return null
    return FirestoreProduct(
        id = id,
        brandId = brandId,
        name = string("name"),
        slug = string("slug"),
        description = string("description"),
        imageUrl = optionalString("imageUrl"),
        barcode = optionalString("barcode"),
        origin = string("origin"),
        roastLevel = string("roastLevel"),
        process = string("process"),
        tastingNotes = stringList("tastingNotes"),
        sourceUrl = optionalString("sourceUrl"),
        importedVia = optionalString("importedVia"),
        averageRating = double("averageRating"),
        reviewCount = int("reviewCount"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreReview(
    val id: String,
    val userId: String,
    val targetType: String,
    val targetId: String,
    val brandId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "targetType" to targetType,
        "targetId" to targetId,
        "brandId" to brandId,
        "rating" to rating,
        "comment" to comment,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreReview(): FirestoreReview? {
    val id = requiredId() ?: return null
    val userId = string("userId")
    val targetType = string("targetType")
    val targetId = string("targetId")
    if (userId.isBlank() || targetType.isBlank() || targetId.isBlank()) return null
    return FirestoreReview(
        id = id,
        userId = userId,
        targetType = targetType,
        targetId = targetId,
        brandId = string("brandId"),
        rating = int("rating"),
        comment = string("comment"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreEvent(
    val id: String,
    val title: String = "",
    val description: String = "",
    val createdBy: String = "",
    val city: String = "",
    val country: String = "",
    val locationName: String = "",
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val participantCount: Int = 0,
    val participantLimit: Int = 0,
    val visibility: String = "public",
    val imageUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "createdBy" to createdBy,
        "city" to city,
        "country" to country,
        "locationName" to locationName,
        "startAt" to startAt,
        "endAt" to endAt,
        "participantCount" to participantCount,
        "participantLimit" to participantLimit,
        "visibility" to visibility,
        "imageUrl" to imageUrl,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreEvent(): FirestoreEvent? {
    val id = requiredId() ?: return null
    return FirestoreEvent(
        id = id,
        title = string("title"),
        description = string("description"),
        createdBy = string("createdBy"),
        city = string("city"),
        country = string("country"),
        locationName = string("locationName"),
        startAt = long("startAt"),
        endAt = long("endAt"),
        participantCount = int("participantCount"),
        participantLimit = int("participantLimit"),
        visibility = string("visibility").ifBlank { "public" },
        imageUrl = optionalString("imageUrl"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}

data class FirestoreClaimRequest(
    val id: String,
    val brandId: String,
    val userId: String,
    val businessName: String = "",
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val instagram: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "brandId" to brandId,
        "userId" to userId,
        "businessName" to businessName,
        "contactName" to contactName,
        "email" to email,
        "phone" to phone,
        "website" to website,
        "instagram" to instagram,
        "status" to status,
        "adminNote" to adminNote,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

fun DocumentSnapshot.toFirestoreClaimRequest(): FirestoreClaimRequest? {
    val id = requiredId() ?: return null
    val brandId = string("brandId")
    val userId = string("userId")
    if (brandId.isBlank() || userId.isBlank()) return null
    return FirestoreClaimRequest(
        id = id,
        brandId = brandId,
        userId = userId,
        businessName = string("businessName"),
        contactName = string("contactName"),
        email = string("email"),
        phone = string("phone"),
        website = string("website"),
        instagram = string("instagram"),
        status = string("status").ifBlank { "pending" },
        adminNote = string("adminNote"),
        createdAt = long("createdAt"),
        updatedAt = long("updatedAt")
    )
}
