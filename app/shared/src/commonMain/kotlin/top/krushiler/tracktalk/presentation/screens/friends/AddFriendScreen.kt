package top.krushiler.tracktalk.presentation.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.add_friend
import tracktalk.app.shared.generated.resources.back
import tracktalk.app.shared.generated.resources.friend_request_accepted
import tracktalk.app.shared.generated.resources.friend_request_cancelled
import tracktalk.app.shared.generated.resources.friend_request_declined
import tracktalk.app.shared.generated.resources.friend_request_sent
import tracktalk.app.shared.generated.resources.incoming
import tracktalk.app.shared.generated.resources.no_incoming_requests
import tracktalk.app.shared.generated.resources.no_outgoing_requests
import tracktalk.app.shared.generated.resources.outgoing
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons

@Composable
fun AddFriendRoute(
    onBack: () -> Unit,
    viewModel: AddFriendViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    AddFriendScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSendRequest = viewModel::sendRequest,
        onAccept = viewModel::accept,
        onReject = viewModel::reject,
        onCancel = viewModel::cancel,
        onClearNotice = viewModel::clearNotice,
    )
}

@Composable
fun AddFriendScreen(
    state: AddFriendUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendRequest: (String) -> Unit,
    onAccept: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onClearNotice: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val notice = state.notice
    val message = notice?.let { noticeText(it.second) }
    LaunchedEffect(notice?.first) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onClearNotice()
        }
    }
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(AppColors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            AddFriendHeader(onBack = onBack, onAdd = { showAdd = true })
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                ) {
                    FriendPeopleSection(
                        title = stringResource(Res.string.incoming),
                        state = state.incoming,
                        empty = stringResource(Res.string.no_incoming_requests),
                        people = (state.incoming as? LoadState.Ready)?.value,
                    ) { friend ->
                        IncomingTile(
                            friend = friend,
                            busy = state.actingOnUserId == friend.id,
                            onAccept = { onAccept(friend.id) },
                            onReject = { onReject(friend.id) },
                        )
                    }
                    FriendPeopleSection(
                        title = stringResource(Res.string.outgoing),
                        state = state.outgoing,
                        empty = stringResource(Res.string.no_outgoing_requests),
                        people = (state.outgoing as? LoadState.Ready)?.value,
                    ) { friend ->
                        OutgoingTile(
                            friend = friend,
                            busy = state.actingOnUserId == friend.id,
                            onCancel = { onCancel(friend.id) },
                        )
                    }
                }
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
        )
    }
    if (showAdd) {
        AddFriendSheet(
            sending = state.isSendingInvite,
            onDismiss = { showAdd = false },
            onSend = {
                onSendRequest(it)
                showAdd = false
            },
        )
    }
}

@Composable
private fun noticeText(notice: AddFriendNotice): String = when (notice) {
    AddFriendNotice.RequestSent -> stringResource(Res.string.friend_request_sent)
    AddFriendNotice.Accepted -> stringResource(Res.string.friend_request_accepted)
    AddFriendNotice.Declined -> stringResource(Res.string.friend_request_declined)
    AddFriendNotice.Cancelled -> stringResource(Res.string.friend_request_cancelled)
    is AddFriendNotice.Error -> notice.message
}

@Composable
private fun AddFriendHeader(onBack: () -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.chevronLeft,
            contentDescription = stringResource(Res.string.back),
            tint = AppColors.primary,
            modifier = Modifier.size(40.dp).clickable(onClick = onBack).padding(6.dp),
        )
        Text(
            stringResource(Res.string.add_friend),
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            color = AppColors.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        AddFriendButton(onClick = onAdd)
    }
}
