package top.krushiler.tracktalk.presentation.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import tracktalk.app.shared.generated.resources.can_see_your_location
import tracktalk.app.shared.generated.resources.friends
import tracktalk.app.shared.generated.resources.location_hidden_from_them
import tracktalk.app.shared.generated.resources.location_shared
import tracktalk.app.shared.generated.resources.location_sharing_stopped
import tracktalk.app.shared.generated.resources.message
import tracktalk.app.shared.generated.resources.no_friends_found
import tracktalk.app.shared.generated.resources.no_friends_yet
import tracktalk.app.shared.generated.resources.search_friends
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.navigation.toPerson
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.appTextFieldColors

@Composable
fun FriendsRoute(
    onOpenChat: (PersonRef) -> Unit,
    onOpenHistory: (PersonRef) -> Unit,
    onAddFriend: () -> Unit,
    viewModel: FriendsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.onShown() }
    FriendsScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onToggleShare = viewModel::toggleShare,
        onClearNotice = viewModel::clearNotice,
        onOpenChat = onOpenChat,
        onOpenHistory = onOpenHistory,
        onAddFriend = onAddFriend,
    )
}

@Composable
fun FriendsScreen(
    state: FriendsUiState,
    onRefresh: () -> Unit,
    onToggleShare: (Long, Boolean) -> Unit,
    onClearNotice: () -> Unit,
    onOpenChat: (PersonRef) -> Unit,
    onOpenHistory: (PersonRef) -> Unit,
    onAddFriend: () -> Unit,
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
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(AppColors.canvas)) {
        Column(Modifier.fillMaxSize()) {
            FriendsHeader(query = query, onQuery = { query = it }, onAdd = onAddFriend)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                val friends = state.friends
                val filtered = (friends as? LoadState.Ready)?.value?.filter { friend ->
                    val text = query.trim().lowercase()
                    if (text.isEmpty()) true
                    else {
                        val name = friend.username.lowercase()
                        name.contains(text) || friend.login.lowercase().contains(text)
                    }
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 96.dp),
                ) {
                    FriendPeopleSection(
                        title = stringResource(Res.string.friends),
                        state = friends,
                        empty = if (query.isBlank()) {
                            stringResource(Res.string.no_friends_yet)
                        } else {
                            stringResource(Res.string.no_friends_found, query)
                        },
                        people = filtered,
                    ) { friend ->
                        FriendTile(
                            friend = friend,
                            busy = state.actingOnUserId == friend.id,
                            onOpenHistory = { onOpenHistory(friend.toPerson()) },
                            onOpenChat = { onOpenChat(friend.toPerson()) },
                            onToggleShare = { onToggleShare(friend.id, it) },
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
private fun noticeText(notice: FriendsNotice): String = when (notice) {
    FriendsNotice.Shared -> stringResource(Res.string.location_shared)
    FriendsNotice.Unshared -> stringResource(Res.string.location_sharing_stopped)
    is FriendsNotice.Error -> notice.message
}

@Composable
private fun FriendsHeader(query: String, onQuery: (String) -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.friends),
                modifier = Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
            )
            AddFriendButton(onClick = onAdd)
        }
        TextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            placeholder = { Text(stringResource(Res.string.search_friends)) },
            leadingIcon = { Icon(AppIcons.search, contentDescription = null, tint = AppColors.slate400) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = appTextFieldColors(),
        )
    }
}

@Composable
private fun FriendTile(
    friend: Friend,
    busy: Boolean,
    onOpenHistory: () -> Unit,
    onOpenChat: () -> Unit,
    onToggleShare: (Boolean) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Box(Modifier.clickable(onClick = onOpenHistory).padding(vertical = 4.dp)) { UserRow(friend) }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenChat)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.chat, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(20.dp))
            Text(stringResource(Res.string.message), modifier = Modifier.weight(1f).padding(start = 10.dp), color = AppColors.text, fontSize = 14.sp)
            Icon(AppIcons.chevronRight, contentDescription = null, tint = AppColors.slate400, modifier = Modifier.size(20.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                AppIcons.locationOnOutlined,
                contentDescription = null,
                tint = if (friend.locationShared) AppColors.primary else AppColors.slate400,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(if (friend.locationShared) Res.string.can_see_your_location else Res.string.location_hidden_from_them),
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                color = AppColors.text,
                fontSize = 14.sp,
            )
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AppColors.primary)
            } else {
                Switch(checked = friend.locationShared, onCheckedChange = onToggleShare)
            }
        }
    }
}
