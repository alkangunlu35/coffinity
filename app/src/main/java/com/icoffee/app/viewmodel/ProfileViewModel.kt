package com.icoffee.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.icoffee.app.data.model.CoffeeMeet
import kotlinx.coroutines.launch
import com.icoffee.app.data.CountryBeansRepository
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.model.CoffeeType
import com.icoffee.app.data.model.TasteNote
import com.icoffee.app.data.model.UserTasteProfile
import com.icoffee.app.data.model.UserTasteSummary
import com.icoffee.app.data.model.toSummary
import com.icoffee.app.data.model.topCoffeeTypes
import com.icoffee.app.data.model.topEventPurposes
import com.icoffee.app.data.model.topOrigins
import com.icoffee.app.data.model.topPreferredNotes
import com.icoffee.app.data.profile.FavoriteBeanItem
import com.icoffee.app.data.profile.FavoriteMenuPick
import com.icoffee.app.data.profile.FavoriteScanProduct
import com.icoffee.app.data.profile.UserTasteProfileRepository

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val tasteProfile: UserTasteProfile = UserTasteProfile(),
    val tasteSummary: UserTasteSummary = UserTasteProfile().toSummary(),
    val favoriteNotes: List<TasteNote> = emptyList(),
    val favoriteOrigins: List<String> = emptyList(),
    val favoriteCoffeeTypes: List<CoffeeType> = emptyList(),
    val favoriteScans: List<FavoriteScanProduct> = emptyList(),
    val favoriteBeans: List<FavoriteBeanItem> = emptyList(),
    val favoriteMenuPicks: List<FavoriteMenuPick> = emptyList(),
    val joinedEvents: List<ProfileEventItem> = emptyList(),
    val createdEvents: List<ProfileEventItem> = emptyList(),
    val scanHistoryCount: Int = 0,
    val eventsByPurpose: List<String> = emptyList()
)

data class ProfileEventItem(
    val id: String,
    val title: String,
    val purpose: String,
    val time: String,
    val location: String,
    val participantsLabel: String
)

class ProfileViewModel : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    private val currentMeetUserId: String
        get() = FirebaseAuthRepository.currentUser?.uid ?: "guest"

    init {
        refresh()
        viewModelScope.launch {
            MeetRepository.eventsFlow.collect { events ->
                updateEventLists(events)
            }
        }
    }

    private fun updateEventLists(events: List<CoffeeMeet>) {
        val joined = events
            .filter { meet ->
                currentMeetUserId in meet.participants && meet.hostId != currentMeetUserId
            }
            .map { meet ->
                ProfileEventItem(
                    id = meet.id,
                    title = meet.title,
                    purpose = meet.purpose,
                    time = meet.time,
                    location = meet.locationName,
                    participantsLabel = "${meet.participants.size}/${meet.maxParticipants}"
                )
            }

        val created = events
            .filter { it.hostId == currentMeetUserId }
            .map { meet ->
                ProfileEventItem(
                    id = meet.id,
                    title = meet.title,
                    purpose = meet.purpose,
                    time = meet.time,
                    location = meet.locationName,
                    participantsLabel = "${meet.participants.size}/${meet.maxParticipants}"
                )
            }

        uiState = uiState.copy(
            joinedEvents = joined,
            createdEvents = created
        )
    }

    fun refresh() {
        val user = FirebaseAuthRepository.currentUser
        val profile = UserTasteProfileRepository.currentProfile()
        val summary = profile.toSummary()

        val favoriteScans = UserTasteProfileRepository.favoriteScans()
        val scanHistoryCount = UserTasteProfileRepository.recentScanHistory(limit = 100).size
        val favoriteBeans = UserTasteProfileRepository.favoriteBeans().ifEmpty {
            suggestBeanFavorites(summary.topOrigins)
        }
        val favoriteMenuPicks = UserTasteProfileRepository.favoriteMenuPicks()

        uiState = uiState.copy(
            user = user,
            tasteProfile = profile,
            tasteSummary = summary,
            favoriteNotes = profile.topPreferredNotes(limit = 5),
            favoriteOrigins = profile.topOrigins(limit = 5),
            favoriteCoffeeTypes = profile.topCoffeeTypes(limit = 4),
            favoriteScans = favoriteScans,
            favoriteBeans = favoriteBeans,
            favoriteMenuPicks = favoriteMenuPicks,
            scanHistoryCount = scanHistoryCount,
            eventsByPurpose = profile.topEventPurposes(limit = 3)
        )
    }

    private fun suggestBeanFavorites(topOrigins: List<String>): List<FavoriteBeanItem> {
        if (topOrigins.isEmpty()) return emptyList()
        return topOrigins.mapNotNull { origin ->
            val country = CountryBeansRepository.getCountry(origin) ?: return@mapNotNull null
            val variety = country.varieties.firstOrNull() ?: return@mapNotNull null
            FavoriteBeanItem(
                id = "suggested_${country.id}_${variety.name}",
                title = variety.name,
                subtitle = variety.flavorNotes.take(2).joinToString(" • "),
                origin = country.country,
                savedAt = 0L
            )
        }
    }
}
