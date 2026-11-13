package top.krushiler.tracktalk.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.data.repository.AuthRepository
import top.krushiler.tracktalk.model.User
import top.krushiler.tracktalk.platform.AvatarPick
import top.krushiler.tracktalk.platform.AvatarPicker
import top.krushiler.tracktalk.platform.clearBackgroundLocationSync
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.userMessage

sealed interface ProfileNotice {
    data object AvatarUpdated : ProfileNotice
    data object UsernameUpdated : ProfileNotice
    data object ComingSoon : ProfileNotice
    data class Error(val message: String) : ProfileNotice
}

data class ProfileUiState(
    val user: LoadState<User> = LoadState.Loading,
    val isUploadingAvatar: Boolean = false,
    val isUpdatingUsername: Boolean = false,
    val loggedOut: Boolean = false,
    val notice: Pair<Long, ProfileNotice>? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val avatarPicker: AvatarPicker,
    private val realtime: RealtimeClient,
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState>
        field = MutableStateFlow(ProfileUiState())

    private var noticeId = 0L

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { authRepository.me() }
                .onSuccess { user -> uiState.update { it.copy(user = LoadState.Ready(user)) } }
                .onFailure { error -> uiState.update { it.copy(user = LoadState.Failed(error.userMessage())) } }
        }
    }

    fun pickAvatar() {
        if (uiState.value.isUploadingAvatar) return
        viewModelScope.launch {
            when (val picked = avatarPicker.pick()) {
                is AvatarPick.Image -> upload(picked.bytes, picked.contentType)
                AvatarPick.Unsupported -> uiState.update { it.copy(notice = next(ProfileNotice.ComingSoon)) }
                AvatarPick.Cancelled -> Unit
            }
        }
    }

    fun updateUsername(username: String) {
        val trimmed = username.trim()
        if (trimmed.isEmpty() || uiState.value.isUpdatingUsername) return
        viewModelScope.launch {
            uiState.update { it.copy(isUpdatingUsername = true) }
            runCatching { authRepository.updateUsername(trimmed) }
                .onSuccess { user ->
                    uiState.update {
                        it.copy(
                            isUpdatingUsername = false,
                            user = LoadState.Ready(user),
                            notice = next(ProfileNotice.UsernameUpdated),
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isUpdatingUsername = false,
                            notice = next(ProfileNotice.Error(error.userMessage())),
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            clearBackgroundLocationSync()
            realtime.stop()
            authRepository.logout()
            uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun clearNotice() {
        uiState.update { it.copy(notice = null) }
    }

    private suspend fun upload(bytes: ByteArray, contentType: String) {
        uiState.update { it.copy(isUploadingAvatar = true) }
        runCatching { authRepository.updateAvatar(bytes, contentType) }
            .onSuccess { user ->
                uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        user = LoadState.Ready(user),
                        notice = next(ProfileNotice.AvatarUpdated),
                    )
                }
            }
            .onFailure { error ->
                uiState.update {
                    it.copy(
                        isUploadingAvatar = false,
                        notice = next(ProfileNotice.Error(error.userMessage())),
                    )
                }
            }
    }

    private fun next(notice: ProfileNotice): Pair<Long, ProfileNotice> {
        noticeId += 1
        return noticeId to notice
    }
}
