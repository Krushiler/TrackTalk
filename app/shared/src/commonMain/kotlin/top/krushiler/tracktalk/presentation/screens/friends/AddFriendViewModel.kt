package top.krushiler.tracktalk.presentation.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.repository.FriendsRepository
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.userMessage

sealed interface AddFriendNotice {
    data object RequestSent : AddFriendNotice
    data object Accepted : AddFriendNotice
    data object Declined : AddFriendNotice
    data object Cancelled : AddFriendNotice
    data class Error(val message: String) : AddFriendNotice
}

data class AddFriendUiState(
    val incoming: LoadState<List<Friend>> = LoadState.Loading,
    val outgoing: LoadState<List<Friend>> = LoadState.Loading,
    val isRefreshing: Boolean = false,
    val isSendingInvite: Boolean = false,
    val actingOnUserId: Long? = null,
    val notice: Pair<Long, AddFriendNotice>? = null,
)

class AddFriendViewModel(
    private val friendsRepository: FriendsRepository,
) : ViewModel() {
    val uiState: StateFlow<AddFriendUiState>
        field = MutableStateFlow(AddFriendUiState())

    private var noticeId = 0L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.update { it.copy(isRefreshing = true) }
            val loaded = runCatching {
                coroutineScope {
                    val incoming = async { friendsRepository.incoming() }
                    val outgoing = async { friendsRepository.outgoing() }
                    incoming.await() to outgoing.await()
                }
            }
            loaded.onSuccess { (incoming, outgoing) ->
                uiState.update {
                    it.copy(
                        isRefreshing = false,
                        incoming = LoadState.Ready(incoming),
                        outgoing = LoadState.Ready(outgoing),
                    )
                }
            }.onFailure { error ->
                val failed = LoadState.Failed(error.userMessage())
                uiState.update {
                    it.copy(
                        isRefreshing = false,
                        incoming = if (it.incoming is LoadState.Ready) it.incoming else failed,
                        outgoing = if (it.outgoing is LoadState.Ready) it.outgoing else failed,
                    )
                }
            }
        }
    }

    fun sendRequest(login: String) {
        val trimmed = login.trim()
        if (trimmed.isEmpty() || uiState.value.isSendingInvite) return
        viewModelScope.launch {
            uiState.update { it.copy(isSendingInvite = true) }
            runCatching { friendsRepository.sendRequest(trimmed) }
                .onSuccess {
                    uiState.update { it.copy(isSendingInvite = false, notice = next(AddFriendNotice.RequestSent)) }
                    refreshOutgoing()
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(isSendingInvite = false, notice = next(AddFriendNotice.Error(error.userMessage())))
                    }
                }
        }
    }

    fun accept(userId: Long) = act(userId, AddFriendNotice.Accepted) {
        friendsRepository.accept(userId)
        refreshIncoming()
    }

    fun reject(userId: Long) = act(userId, AddFriendNotice.Declined) {
        friendsRepository.reject(userId)
        refreshIncoming()
    }

    fun cancel(userId: Long) = act(userId, AddFriendNotice.Cancelled) {
        friendsRepository.cancel(userId)
        refreshOutgoing()
    }

    fun clearNotice() {
        uiState.update { it.copy(notice = null) }
    }

    private fun act(userId: Long, notice: AddFriendNotice, block: suspend () -> Unit) {
        if (uiState.value.actingOnUserId != null) return
        viewModelScope.launch {
            uiState.update { it.copy(actingOnUserId = userId) }
            runCatching { block() }
                .onSuccess { uiState.update { it.copy(actingOnUserId = null, notice = next(notice)) } }
                .onFailure { error ->
                    uiState.update {
                        it.copy(actingOnUserId = null, notice = next(AddFriendNotice.Error(error.userMessage())))
                    }
                }
        }
    }

    private fun next(notice: AddFriendNotice): Pair<Long, AddFriendNotice> {
        noticeId += 1
        return noticeId to notice
    }

    private fun refreshIncoming() {
        viewModelScope.launch {
            runCatching { friendsRepository.incoming() }.onSuccess { friends ->
                uiState.update { it.copy(incoming = LoadState.Ready(friends)) }
            }
        }
    }

    private fun refreshOutgoing() {
        viewModelScope.launch {
            runCatching { friendsRepository.outgoing() }.onSuccess { friends ->
                uiState.update { it.copy(outgoing = LoadState.Ready(friends)) }
            }
        }
    }
}
