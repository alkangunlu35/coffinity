package com.icoffee.app.data.model

enum class AppUserRole(val storageValue: String) {
    SUPER_ADMIN("super_admin"),
    BRAND_ADMIN("brand_admin"),
    USER("user");

    companion object {
        fun fromStorage(raw: String?): AppUserRole {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageValue == normalized } ?: USER
        }
    }
}

