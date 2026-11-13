package top.krushiler.tracktalk.presentation.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.data.repository.ChatRepository
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.userMessage
import top.krushiler.tracktalk.ws.RealtimeEvent

data class ChatUiState(
    val person: PersonRef,
    val messages: LoadState<List<ChatMessage>> = LoadState.Loading,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    person: PersonRef,
    private val chatRepository: ChatRepository,
    private val realtime: RealtimeClient,
) : ViewModel() {
    val uiState: StateFlow<ChatUiState>
        field = MutableStateFlow(ChatUiState(person = person))

    init {
        refresh()
        viewModelScope.launch {
            realtime.ensureStarted()
            realtime.events.collect { event ->
                val incoming = event as? RealtimeEvent.ChatMessage ?: return@collect
                if (incoming.data.userId != person.userId) return@collect
                runCatching { chatRepository.message(incoming.data.messageId) }
                    .onSuccess { message -> prepend(message) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState.update { it.copy(messages = LoadState.Loading, error = null) }
            runCatching { chatRepository.messages(uiState.value.person.userId) }
                .onSuccess { page ->
                    uiState.update {
                        it.copy(messages = LoadState.Ready(page), hasMore = page.size >= PageSize)
                    }
                }
                .onFailure { error ->
                    uiState.update { it.copy(messages = LoadState.Failed(error.userMessage())) }
                }
        }
    }

    fun loadMore() {
        val state = uiState.value
        val current = state.messages as? LoadState.Ready ?: return
        if (!state.hasMore || state.isLoadingMore || current.value.isEmpty()) return
        viewModelScope.launch {
            uiState.update { it.copy(isLoadingMore = true) }
            val oldestId = current.value.last().id
            runCatching { chatRepository.messages(state.person.userId, beforeId = oldestId) }
                .onSuccess { page ->
                    uiState.update { latest ->
                        val existing = (latest.messages as? LoadState.Ready)?.value.orEmpty()
                        val known = existing.map { it.id }.toSet()
                        latest.copy(
                            isLoadingMore = false,
                            hasMore = page.size >= PageSize,
                            messages = LoadState.Ready(existing + page.filter { it.id !in known }),
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update { it.copy(isLoadingMore = false, error = error.userMessage()) }
                }
        }
    }

    fun send(body: String) {
        val text = body.trim()
        if (text.isEmpty() || uiState.value.isSending) return
        viewModelScope.launch {
            uiState.update { it.copy(isSending = true, error = null) }
            runCatching { chatRepository.send(uiState.value.person.userId, text) }
                .onSuccess { message ->
                    prepend(message)
                    uiState.update { it.copy(isSending = false) }
                }
                .onFailure { error ->
                    uiState.update { it.copy(isSending = false, error = error.userMessage()) }
                }
        }
    }

    fun clearError() {
        uiState.update { it.copy(error = null) }
    }

    private fun prepend(message: ChatMessage) {
        uiState.update { state ->
            val existing = (state.messages as? LoadState.Ready)?.value.orEmpty()
            if (existing.any { it.id == message.id }) state
            else state.copy(messages = LoadState.Ready(listOf(message) + existing))
        }
    }

    private companion object {
        const val PageSize = 50
    }
}
