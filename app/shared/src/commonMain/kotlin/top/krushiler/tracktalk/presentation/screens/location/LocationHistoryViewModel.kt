package top.krushiler.tracktalk.presentation.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.data.repository.FriendsRepository
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.model.LocationLogEntry
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.navigation.toFriend
import top.krushiler.tracktalk.presentation.userMessage
import top.krushiler.tracktalk.ws.RealtimeEvent

data class LocationHistoryUiState(
    val person: PersonRef,
    val from: Instant,
    val to: Instant? = null,
    val isLoading: Boolean = true,
    val entries: List<LocationLogEntry> = emptyList(),
    val selected: LocationLogEntry? = null,
    val live: FriendLocation? = null,
    val error: String? = null,
)

class LocationHistoryViewModel(
    person: PersonRef,
    private val friendsRepository: FriendsRepository,
    private val realtime: RealtimeClient,
) : ViewModel() {
    val uiState: StateFlow<LocationHistoryUiState>
        field = MutableStateFlow(
            LocationHistoryUiState(
                person = person,
                from = Clock.System.now() - 1.hours,
            ),
        )

    init {
        load()
        viewModelScope.launch {
            realtime.ensureStarted()
            realtime.events.collect { event ->
                val update = event as? RealtimeEvent.FriendLocationUpdate ?: return@collect
                if (update.data.userId != person.userId) return@collect
                uiState.update { state ->
                    state.copy(
                        live = FriendLocation(
                            user = state.live?.user ?: state.person.toFriend(),
                            latitude = update.data.latitude,
                            longitude = update.data.longitude,
                            updatedAt = Clock.System.now(),
                        ),
                    )
                }
            }
        }
    }

    fun load() {
        val state = uiState.value
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null, selected = null) }
            runCatching { friendsRepository.locationLog(state.person.userId, state.from, state.to) }
                .onSuccess { entries ->
                    val live = runCatching { friendsRepository.friendLocation(state.person.userId) }.getOrNull()
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = entries.sortedBy { entry -> entry.recordedAt },
                            live = live,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update { it.copy(isLoading = false, error = error.userMessage()) }
                }
        }
    }

    fun changeRange(from: Instant, to: Instant?) {
        if (to != null && from >= to) {
            uiState.update { it.copy(error = "invalid-range") }
            return
        }
        uiState.update { it.copy(from = from, to = to, error = null) }
        load()
    }

    fun select(entry: LocationLogEntry?) {
        uiState.update { it.copy(selected = entry) }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }
}
