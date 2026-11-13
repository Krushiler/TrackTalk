package top.krushiler.tracktalk.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.network.AccessTokenStore
import top.krushiler.tracktalk.data.repository.FriendsRepository
import top.krushiler.tracktalk.data.repository.LocationRepository
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.geo.GeoPoint
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.model.Location
import top.krushiler.tracktalk.platform.DeviceLocation
import top.krushiler.tracktalk.platform.LocationAccess
import top.krushiler.tracktalk.platform.requestLocationQuickTile
import top.krushiler.tracktalk.platform.startBackgroundLocationSync
import top.krushiler.tracktalk.presentation.userMessage
import top.krushiler.tracktalk.ws.RealtimeEvent

data class MapUiState(
    val friends: List<FriendLocation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class MapViewModel(
    private val friendsRepository: FriendsRepository,
    private val realtime: RealtimeClient,
) : ViewModel() {
    val uiState: StateFlow<MapUiState>
        field = MutableStateFlow(MapUiState())

    init {
        refresh()
        viewModelScope.launch {
            realtime.events.collect { event ->
                if (event is RealtimeEvent.FriendLocationUpdate) {
                    uiState.update { state ->
                        state.copy(
                            friends = state.friends.map { friend ->
                                if (friend.user.id != event.data.userId) friend
                                else friend.copy(
                                    latitude = event.data.latitude,
                                    longitude = event.data.longitude,
                                    updatedAt = Clock.System.now(),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = it.friends.isEmpty(), error = null) }
            runCatching { friendsRepository.friendLocations() }
                .onSuccess { locations -> uiState.update { it.copy(isLoading = false, friends = locations) } }
                .onFailure { error -> uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
        }
    }
}

data class LocationSyncUiState(
    val isSyncing: Boolean = false,
    val location: GeoPoint? = null,
    val error: String? = null,
)

class LocationSyncViewModel(
    private val deviceLocation: DeviceLocation,
    private val locationRepository: LocationRepository,
    private val accessToken: AccessTokenStore,
    private val baseUrl: String,
) : ViewModel() {
    val permission: StateFlow<LocationAccess> = deviceLocation.permission
    val uiState: StateFlow<LocationSyncUiState>
        field = MutableStateFlow(LocationSyncUiState())

    private var syncJob: Job? = null
    private var askedAlways = false

    init {
        viewModelScope.launch {
            deviceLocation.permission.collect { access ->
                when (access) {
                    LocationAccess.WhileInUse -> {
                        startSyncing()
                        if (!askedAlways) {
                            askedAlways = true
                            deviceLocation.requestAlways()
                        }
                    }
                    LocationAccess.Always -> {
                        startSyncing()
                        startBackground()
                    }
                    LocationAccess.Unknown, LocationAccess.Denied -> Unit
                }
            }
        }
    }

    fun requestPermission() {
        viewModelScope.launch { deviceLocation.requestWhileInUse() }
    }

    fun openSettings() {
        deviceLocation.openSettings()
    }

    fun onAppResumed() {
        deviceLocation.refresh()
        if (permission.value == LocationAccess.Always) {
            startBackground()
        }
    }

    private fun startBackground() {
        val token = accessToken.token ?: return
        if (baseUrl.isBlank()) return
        viewModelScope.launch {
            startBackgroundLocationSync(token, baseUrl)
            requestLocationQuickTile()
        }
    }

    private fun startSyncing() {
        if (syncJob?.isActive == true) return
        uiState.update { it.copy(isSyncing = true) }
        syncJob = viewModelScope.launch {
            deviceLocation.fixes.collect { point ->
                uiState.update { it.copy(isSyncing = true, location = point, error = null) }
                runCatching {
                    locationRepository.sync(Location(point.latitude, point.longitude))
                }.onFailure { error ->
                    uiState.update { it.copy(error = error.userMessage()) }
                }
            }
        }
    }

    override fun onCleared() {
        deviceLocation.stop()
        super.onCleared()
    }
}
