package top.krushiler.tracktalk.presentation.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.repository.FriendsRepository
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.userMessage

sealed interface FriendsNotice {
    data object Shared : FriendsNotice
    data object Unshared : FriendsNotice
    data class Error(val message: String) : FriendsNotice
}

data class FriendsUiState(
    val friends: LoadState<List<Friend>> = LoadState.Loading,
    val isRefreshing: Boolean = false,
    val actingOnUserId: Long? = null,
    val notice: Pair<Long, FriendsNotice>? = null,
)

class FriendsViewModel(
    private val friendsRepository: FriendsRepository,
) : ViewModel() {
    val uiState: StateFlow<FriendsUiState>
        field = MutableStateFlow(FriendsUiState())

    private var noticeId = 0L
    private var hasBeenShown = false

    init {
        refresh()
    }

    fun onShown() {
        if (hasBeenShown) refresh()
        hasBeenShown = true
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.update { it.copy(isRefreshing = true) }
            runCatching { friendsRepository.friends() }
                .onSuccess { friends ->
                    uiState.update { it.copy(isRefreshing = false, friends = LoadState.Ready(friends)) }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isRefreshing = false,
                            friends = if (it.friends is LoadState.Ready) it.friends else LoadState.Failed(error.userMessage()),
                        )
                    }
                }
        }
    }

    fun toggleShare(userId: Long, share: Boolean) = act(
        userId,
        if (share) FriendsNotice.Shared else FriendsNotice.Unshared,
    ) {
        if (share) friendsRepository.shareLocation(userId) else friendsRepository.unshareLocation(userId)
        refreshFriends()
    }

    fun clearNotice() {
        uiState.update { it.copy(notice = null) }
    }

    private fun act(userId: Long, notice: FriendsNotice, block: suspend () -> Unit) {
        if (uiState.value.actingOnUserId != null) return
        viewModelScope.launch {
            uiState.update { it.copy(actingOnUserId = userId) }
            runCatching { block() }
                .onSuccess { uiState.update { it.copy(actingOnUserId = null, notice = next(notice)) } }
                .onFailure { error ->
                    uiState.update {
                        it.copy(actingOnUserId = null, notice = next(FriendsNotice.Error(error.userMessage())))
                    }
                }
        }
    }

    private fun next(notice: FriendsNotice): Pair<Long, FriendsNotice> {
        noticeId += 1
        return noticeId to notice
    }

    private fun refreshFriends() {
        viewModelScope.launch {
            runCatching { friendsRepository.friends() }.onSuccess { friends ->
                uiState.update { it.copy(friends = LoadState.Ready(friends)) }
            }
        }
    }
}
