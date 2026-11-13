package top.krushiler.tracktalk.presentation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.back
import tracktalk.app.shared.generated.resources.retry
import tracktalk.app.shared.generated.resources.say_hello_to
import top.krushiler.tracktalk.model.ChatMessage
import top.krushiler.tracktalk.platform.formatLocalTime
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.util.avatarColor
import top.krushiler.tracktalk.presentation.util.displayName
import top.krushiler.tracktalk.presentation.util.initialsOf
import top.krushiler.tracktalk.presentation.widgets.AppAvatar

@Composable
fun ChatRoute(
    person: PersonRef,
    onBack: () -> Unit,
    viewModel: ChatViewModel = koinViewModel { parametersOf(person) },
) {
    val state by viewModel.uiState.collectAsState()
    ChatScreen(
        state = state,
        onBack = onBack,
        onSend = viewModel::send,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(error)
        onClearError()
    }
    val name = displayName(state.person.username, state.person.login, state.person.userId)
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(AppColors.canvas)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.chevronLeft, contentDescription = stringResource(Res.string.back), tint = AppColors.primary, modifier = Modifier.size(40.dp).clickable(onClick = onBack).padding(6.dp))
            AppAvatar(initials = initialsOf(name), color = avatarColor(state.person.userId), imageUrl = state.person.avatarUrl, size = 40.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(name, color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val login = state.person.login
                if (login.isNotBlank() && login != name) {
                    Text("@$login", color = AppColors.muted, fontSize = 12.sp)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val messages = state.messages) {
                LoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppColors.primary)
                is LoadState.Failed -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(messages.message, color = AppColors.muted)
                    TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
                }
                is LoadState.Ready -> if (messages.value.isEmpty()) {
                    Text(
                        stringResource(Res.string.say_hello_to, name),
                        modifier = Modifier.align(Alignment.Center),
                        color = AppColors.muted,
                    )
                } else {
                    MessageList(messages.value, state.person.userId, state.isLoadingMore, onLoadMore)
                }
            }
        }
        Composer(
            value = draft,
            sending = state.isSending,
            onValue = { draft = it },
            onSend = {
                onSend(draft)
                draft = ""
            },
        )
        SnackbarHost(snackbar)
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    friendId: Long,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearOlder = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= messages.lastIndex - 2 } == true
    LaunchedEffect(nearOlder, messages.size) {
        if (nearOlder) onLoadMore()
    }
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages.size, key = { messages[it].id }) { index ->
            val message = messages[index]
            Bubble(message, incoming = message.senderId == friendId)
        }
        if (loadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = AppColors.primary)
                }
            }
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage, incoming: Boolean) {
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (incoming) 4.dp else 16.dp,
        bottomEnd = if (incoming) 16.dp else 4.dp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(if (incoming) Color.White else AppColors.primary, shape)
                .then(if (incoming) Modifier.border(1.dp, AppColors.border, shape) else Modifier)
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp),
        ) {
            Text(message.body, color = if (incoming) AppColors.text else Color.White, fontSize = 15.sp)
            Text(
                formatLocalTime(message.createdAt.toEpochMilliseconds()),
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                color = if (incoming) AppColors.slate400 else Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun Composer(value: String, sending: Boolean, onValue: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier
                .weight(1f)
                .background(AppColors.inputFill, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = AppColors.text, fontSize = 15.sp),
            cursorBrush = SolidColor(AppColors.primary),
            maxLines = 4,
        )
        Box(
            Modifier
                .padding(start = 8.dp)
                .size(44.dp)
                .background(AppColors.primary, RoundedCornerShape(14.dp))
                .clickable(enabled = value.isNotBlank() && !sending, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (sending) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(AppIcons.send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
