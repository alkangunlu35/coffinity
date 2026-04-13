package com.icoffee.app.data.profile

import android.content.Context
import android.content.SharedPreferences
import com.icoffee.app.data.model.AcidityLevel
import com.icoffee.app.data.model.CoffeeProfile
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.RoastLevel
import com.icoffee.app.data.model.StrengthLevel
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import org.json.JSONArray
import org.json.JSONObject

object UserTasteProfileRepository {
    private const val PREFS_NAME = "coffinity_user_taste_profile"
    private const val KEY_PROFILE_JSON = "profile_json"
    private const val KEY_FAVORITE_SCANS = "favorite_scans"
    private const val KEY_SCAN_HISTORY = "scan_history"
    private const val KEY_FAVORITE_BEANS = "favorite_beans"
    private const val KEY_FAVORITE_MENU_PICKS = "favorite_menu_picks"
    private const val MAX_FAVORITE_SCANS = 30
    private const val MAX_SCAN_HISTORY = 50
    private const val MAX_FAVORITE_BEANS = 40
    private const val MAX_FAVORITE_MENU_PICKS = 40

    private lateinit var prefs: SharedPreferences
    private var inMemoryProfile: UserTasteProfile? = null
    private var favoriteScans: MutableList<FavoriteScanProduct> = mutableListOf()
    private var scanHistory: MutableList<FavoriteScanProduct> = mutableListOf()
    private var favoriteBeans: MutableList<FavoriteBeanItem> = mutableListOf()
    private var favoriteMenuPicks: MutableList<FavoriteMenuPick> = mutableListOf()

    fun initialize(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        inMemoryProfile = loadFromStorage()
        favoriteScans = loadFavoriteScans().toMutableList()
        scanHistory = loadScanHistory().toMutableList()
        favoriteBeans = loadFavoriteBeans().toMutableList()
        favoriteMenuPicks = loadFavoriteMenuPicks().toMutableList()
    }

    fun currentProfile(): UserTasteProfile {
        ensureInitialized()
        return inMemoryProfile ?: loadFromStorage().also { inMemoryProfile = it }
    }

    fun saveProfile(profile: UserTasteProfile) {
        ensureInitialized()
        inMemoryProfile = profile
        prefs.edit().putString(KEY_PROFILE_JSON, profile.toJson()).apply()
    }

    fun resetProfile() {
        ensureInitialized()
        val reset = UserTasteEngine.createDefaultProfile()
        saveProfile(reset)
    }

    fun applyEvent(event: UserTasteEvent): UserTasteProfile {
        val updated = UserTasteEngine.applyEvent(
            current = currentProfile(),
            event = event
        )
        saveProfile(updated)
        return updated
    }

    fun onOnboardingCompleted(
        flavorStyles: List<OnboardingFlavorStyle>,
        preferredRoast: RoastLevel? = null,
        preferredStrength: StrengthLevel? = null,
        preferredAcidity: AcidityLevel? = null,
        prefersMilkDrinks: Boolean? = null
    ): UserTasteProfile = applyEvent(
        UserTasteEvent.OnboardingCompleted(
            flavorStyles = flavorStyles,
            preferredRoast = preferredRoast,
            preferredStrength = preferredStrength,
            preferredAcidity = preferredAcidity,
            prefersMilkDrinks = prefersMilkDrinks
        )
    )

    fun onProductScanned(profile: CoffeeProfile): UserTasteProfile =
        applyEvent(UserTasteEvent.ProductScanned(profile))
            .also { appendScanHistory(profile) }

    fun onProductFavorited(profile: CoffeeProfile): UserTasteProfile =
        applyEvent(UserTasteEvent.ProductFavorited(profile))
            .also { addFavoriteScan(profile) }

    fun onProductUnfavorited(profile: CoffeeProfile): UserTasteProfile =
        applyEvent(UserTasteEvent.ProductUnfavorited(profile))
            .also { removeFavoriteScan(profile.barcode) }

    fun onQuickReaction(profile: CoffeeProfile, reaction: TasteReaction): UserTasteProfile =
        applyEvent(UserTasteEvent.QuickReaction(profile, reaction))

    fun onEventJoined(purpose: String): UserTasteProfile =
        applyEvent(UserTasteEvent.EventJoined(purpose))

    fun onEventCreated(purpose: String): UserTasteProfile =
        applyEvent(UserTasteEvent.EventCreated(purpose))

    fun onMenuItemViewed(profile: CoffeeProfile): UserTasteProfile =
        applyEvent(UserTasteEvent.MenuItemViewed(profile))

    fun onMenuItemFavorited(profile: CoffeeProfile): UserTasteProfile =
        applyEvent(UserTasteEvent.MenuItemFavorited(profile))

    fun favoriteScans(): List<FavoriteScanProduct> {
        ensureInitialized()
        return favoriteScans.sortedByDescending { it.savedAt }
    }

    fun recentScanHistory(limit: Int = 10): List<FavoriteScanProduct> {
        ensureInitialized()
        return scanHistory
            .sortedByDescending { it.savedAt }
            .take(limit)
    }

    fun favoriteBeans(): List<FavoriteBeanItem> {
        ensureInitialized()
        return favoriteBeans.sortedByDescending { it.savedAt }
    }

    fun favoriteMenuPicks(): List<FavoriteMenuPick> {
        ensureInitialized()
        return favoriteMenuPicks.sortedByDescending { it.savedAt }
    }

    fun isFavoriteScan(barcode: String): Boolean {
        ensureInitialized()
        return favoriteScans.any { it.barcode == barcode }
    }

    fun addFavoriteBean(item: FavoriteBeanItem) {
        ensureInitialized()
        favoriteBeans.removeAll { it.id == item.id }
        favoriteBeans.add(0, item.copy(savedAt = System.currentTimeMillis()))
        if (favoriteBeans.size > MAX_FAVORITE_BEANS) {
            favoriteBeans = favoriteBeans.take(MAX_FAVORITE_BEANS).toMutableList()
        }
        persistFavoriteBeans()
    }

    fun removeFavoriteBean(id: String) {
        ensureInitialized()
        favoriteBeans.removeAll { it.id == id }
        persistFavoriteBeans()
    }

    fun addFavoriteMenuPick(item: FavoriteMenuPick) {
        ensureInitialized()
        favoriteMenuPicks.removeAll { it.id == item.id }
        favoriteMenuPicks.add(0, item.copy(savedAt = System.currentTimeMillis()))
        if (favoriteMenuPicks.size > MAX_FAVORITE_MENU_PICKS) {
            favoriteMenuPicks = favoriteMenuPicks.take(MAX_FAVORITE_MENU_PICKS).toMutableList()
        }
        persistFavoriteMenuPicks()
    }

    fun removeFavoriteMenuPick(id: String) {
        ensureInitialized()
        favoriteMenuPicks.removeAll { it.id == id }
        persistFavoriteMenuPicks()
    }

    private fun loadFromStorage(): UserTasteProfile {
        val raw = prefs.getString(KEY_PROFILE_JSON, null)
        if (raw.isNullOrBlank()) return UserTasteEngine.createDefaultProfile()
        return runCatching { raw.toUserTasteProfile() }
            .getOrElse { UserTasteEngine.createDefaultProfile() }
    }

    private fun appendScanHistory(profile: CoffeeProfile) {
        ensureInitialized()
        val snapshot = profile.toScanSnapshot(savedAt = System.currentTimeMillis())
        scanHistory.removeAll { it.barcode == snapshot.barcode }
        scanHistory.add(0, snapshot)
        if (scanHistory.size > MAX_SCAN_HISTORY) {
            scanHistory = scanHistory.take(MAX_SCAN_HISTORY).toMutableList()
        }
        persistScanHistory()
    }

    private fun addFavoriteScan(profile: CoffeeProfile) {
        ensureInitialized()
        val snapshot = profile.toScanSnapshot(savedAt = System.currentTimeMillis())
        favoriteScans.removeAll { it.barcode == snapshot.barcode }
        favoriteScans.add(0, snapshot)
        if (favoriteScans.size > MAX_FAVORITE_SCANS) {
            favoriteScans = favoriteScans.take(MAX_FAVORITE_SCANS).toMutableList()
        }
        persistFavoriteScans()
    }

    private fun removeFavoriteScan(barcode: String) {
        ensureInitialized()
        favoriteScans.removeAll { it.barcode == barcode }
        persistFavoriteScans()
    }

    private fun ensureInitialized() {
        check(::prefs.isInitialized) {
            "UserTasteProfileRepository.initialize(context) must be called before use."
        }
    }

    private fun loadFavoriteScans(): List<FavoriteScanProduct> {
        val raw = prefs.getString(KEY_FAVORITE_SCANS, null) ?: return emptyList()
        return runCatching {
            JSONArray(raw).toFavoriteScanList()
        }.getOrElse { emptyList() }
    }

    private fun loadScanHistory(): List<FavoriteScanProduct> {
        val raw = prefs.getString(KEY_SCAN_HISTORY, null) ?: return emptyList()
        return runCatching {
            JSONArray(raw).toFavoriteScanList()
        }.getOrElse { emptyList() }
    }

    private fun loadFavoriteBeans(): List<FavoriteBeanItem> {
        val raw = prefs.getString(KEY_FAVORITE_BEANS, null) ?: return emptyList()
        return runCatching {
            JSONArray(raw).toFavoriteBeanList()
        }.getOrElse { emptyList() }
    }

    private fun loadFavoriteMenuPicks(): List<FavoriteMenuPick> {
        val raw = prefs.getString(KEY_FAVORITE_MENU_PICKS, null) ?: return emptyList()
        return runCatching {
            JSONArray(raw).toFavoriteMenuPickList()
        }.getOrElse { emptyList() }
    }

    private fun persistFavoriteScans() {
        prefs.edit()
            .putString(KEY_FAVORITE_SCANS, favoriteScans.toFavoriteScanJsonArray().toString())
            .apply()
    }

    private fun persistScanHistory() {
        prefs.edit()
            .putString(KEY_SCAN_HISTORY, scanHistory.toFavoriteScanJsonArray().toString())
            .apply()
    }

    private fun persistFavoriteBeans() {
        prefs.edit()
            .putString(KEY_FAVORITE_BEANS, favoriteBeans.toFavoriteBeanJsonArray().toString())
            .apply()
    }

    private fun persistFavoriteMenuPicks() {
        prefs.edit()
            .putString(KEY_FAVORITE_MENU_PICKS, favoriteMenuPicks.toFavoriteMenuPickJsonArray().toString())
            .apply()
    }

    private fun UserTasteProfile.toJson(): String {
        return JSONObject().apply {
            put("preferredNotes", preferredNotes.toEnumJson())
            put("avoidedNotes", avoidedNotes.toEnumJson())
            put("roastPreference", roastPreference.toEnumJson())
            put("strengthPreference", strengthPreference.toEnumJson())
            put("acidityPreference", acidityPreference.toEnumJson())
            put("milkFriendlyPreferenceScore", milkFriendlyPreferenceScore)
            put("favoriteOrigins", favoriteOrigins.toStringJson())
            put("favoriteCoffeeTypes", favoriteCoffeeTypes.toEnumJson())
            put("eventPurposePreference", eventPurposePreference.toStringJson())
            put("interactionCount", interactionCount)
            put("lastUpdated", lastUpdated)
        }.toString()
    }

    private fun String.toUserTasteProfile(): UserTasteProfile {
        val json = JSONObject(this)
        return UserTasteProfile(
            preferredNotes = json.optJSONObject("preferredNotes").toEnumMap<TasteNote>(),
            avoidedNotes = json.optJSONObject("avoidedNotes").toEnumMap<TasteNote>(),
            roastPreference = json.optJSONObject("roastPreference").toEnumMap<RoastLevel>(),
            strengthPreference = json.optJSONObject("strengthPreference").toEnumMap<StrengthLevel>(),
            acidityPreference = json.optJSONObject("acidityPreference").toEnumMap<AcidityLevel>(),
            milkFriendlyPreferenceScore = json.optInt("milkFriendlyPreferenceScore", 0),
            favoriteOrigins = json.optJSONObject("favoriteOrigins").toStringIntMap(),
            favoriteCoffeeTypes = json.optJSONObject("favoriteCoffeeTypes").toEnumMap<CoffeeType>(),
            eventPurposePreference = json.optJSONObject("eventPurposePreference").toStringIntMap(),
            interactionCount = json.optInt("interactionCount", 0),
            lastUpdated = json.optLong("lastUpdated", 0L)
        )
    }

    private fun Map<String, Int>.toStringJson(): JSONObject =
        JSONObject().also { json ->
            forEach { (key, value) -> json.put(key, value) }
        }

    private fun <T : Enum<T>> Map<T, Int>.toEnumJson(): JSONObject =
        JSONObject().also { json ->
            forEach { (key, value) -> json.put(key.name, value) }
        }

    private fun JSONObject?.toStringIntMap(): Map<String, Int> {
        if (this == null) return emptyMap()
        return keys().asSequence()
            .mapNotNull { key ->
                val value = optInt(key, Int.MIN_VALUE)
                if (value == Int.MIN_VALUE || value <= 0) null else key to value
            }
            .toMap()
    }

    private inline fun <reified T : Enum<T>> JSONObject?.toEnumMap(): Map<T, Int> {
        if (this == null) return emptyMap()
        val valid = enumValues<T>().associateBy { it.name }
        return keys().asSequence()
            .mapNotNull { key ->
                val enumKey = valid[key] ?: return@mapNotNull null
                val value = optInt(key, Int.MIN_VALUE)
                if (value == Int.MIN_VALUE || value <= 0) null else enumKey to value
            }
            .toMap()
    }

    private fun CoffeeProfile.toScanSnapshot(savedAt: Long): FavoriteScanProduct =
        FavoriteScanProduct(
            barcode = barcode,
            name = productName,
            brand = brand,
            origin = originCountry,
            roast = roastLevel.toReadable(),
            imageUrl = imageUrl,
            savedAt = savedAt
        )

    private fun RoastLevel.toReadable(): String = when (this) {
        RoastLevel.LIGHT -> "Light"
        RoastLevel.MEDIUM -> "Medium"
        RoastLevel.DARK -> "Dark"
        RoastLevel.UNKNOWN -> "Unknown"
    }

    private fun List<FavoriteScanProduct>.toFavoriteScanJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("barcode", item.barcode)
                        put("name", item.name)
                        put("brand", item.brand)
                        put("origin", item.origin)
                        put("roast", item.roast)
                        put("imageUrl", item.imageUrl)
                        put("savedAt", item.savedAt)
                    }
                )
            }
        }

    private fun List<FavoriteBeanItem>.toFavoriteBeanJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("subtitle", item.subtitle)
                        put("origin", item.origin)
                        put("savedAt", item.savedAt)
                    }
                )
            }
        }

    private fun List<FavoriteMenuPick>.toFavoriteMenuPickJsonArray(): JSONArray =
        JSONArray().also { array ->
            forEach { item ->
                array.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("subtitle", item.subtitle)
                        put("savedAt", item.savedAt)
                    }
                )
            }
        }

    private fun JSONArray.toFavoriteScanList(): List<FavoriteScanProduct> =
        buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val barcode = item.optString("barcode")
                val name = item.optString("name")
                if (barcode.isBlank() || name.isBlank()) continue
                add(
                    FavoriteScanProduct(
                        barcode = barcode,
                        name = name,
                        brand = item.optString("brand").ifBlank { null },
                        origin = item.optString("origin").ifBlank { null },
                        roast = item.optString("roast").ifBlank { "Unknown" },
                        imageUrl = item.optString("imageUrl").ifBlank { null },
                        savedAt = item.optLong("savedAt", 0L)
                    )
                )
            }
        }

    private fun JSONArray.toFavoriteBeanList(): List<FavoriteBeanItem> =
        buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                val title = item.optString("title")
                if (id.isBlank() || title.isBlank()) continue
                add(
                    FavoriteBeanItem(
                        id = id,
                        title = title,
                        subtitle = item.optString("subtitle").ifBlank { "" },
                        origin = item.optString("origin").ifBlank { "" },
                        savedAt = item.optLong("savedAt", 0L)
                    )
                )
            }
        }

    private fun JSONArray.toFavoriteMenuPickList(): List<FavoriteMenuPick> =
        buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val id = item.optString("id")
                val title = item.optString("title")
                if (id.isBlank() || title.isBlank()) continue
                add(
                    FavoriteMenuPick(
                        id = id,
                        title = title,
                        subtitle = item.optString("subtitle").ifBlank { "" },
                        savedAt = item.optLong("savedAt", 0L)
                    )
                )
            }
        }
}
