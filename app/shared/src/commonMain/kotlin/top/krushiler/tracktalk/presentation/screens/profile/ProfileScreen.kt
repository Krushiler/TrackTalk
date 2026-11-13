package top.krushiler.tracktalk.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.avatar_updated
import tracktalk.app.shared.generated.resources.camera
import tracktalk.app.shared.generated.resources.change_username
import tracktalk.app.shared.generated.resources.change_username_hint
import tracktalk.app.shared.generated.resources.coming_soon
import tracktalk.app.shared.generated.resources.download_app
import tracktalk.app.shared.generated.resources.friends_can_see_your_location
import tracktalk.app.shared.generated.resources.gallery
import tracktalk.app.shared.generated.resources.loading
import tracktalk.app.shared.generated.resources.location_sharing_off
import tracktalk.app.shared.generated.resources.log_out
import tracktalk.app.shared.generated.resources.my_profile
import tracktalk.app.shared.generated.resources.save
import tracktalk.app.shared.generated.resources.sharing_live_location
import tracktalk.app.shared.generated.resources.username
import tracktalk.app.shared.generated.resources.username_updated
import tracktalk.app.shared.generated.resources.your_location_is_hidden
import top.krushiler.tracktalk.platform.isWebTarget
import top.krushiler.tracktalk.platform.openExternalUrl
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.screens.home.LocationSyncUiState
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.GradientButton
import top.krushiler.tracktalk.presentation.theme.LocalDownloadUrl
import top.krushiler.tracktalk.presentation.theme.appTextFieldColors
import top.krushiler.tracktalk.presentation.util.avatarColor
import top.krushiler.tracktalk.presentation.util.displayName
import top.krushiler.tracktalk.presentation.util.initialsOf
import top.krushiler.tracktalk.presentation.widgets.AppAvatar
import top.krushiler.tracktalk.presentation.widgets.HairlineDivider
import top.krushiler.tracktalk.presentation.widgets.MenuRow
import top.krushiler.tracktalk.presentation.widgets.SectionCard
import top.krushiler.tracktalk.presentation.widgets.SheetHandle

@Composable
fun ProfileRoute(
    sharing: LocationSyncUiState,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }
    ProfileScreen(
        state = state,
        sharing = sharing.isSyncing,
        onPickAvatar = viewModel::pickAvatar,
        onUpdateUsername = viewModel::updateUsername,
        onLogout = viewModel::logout,
        onClearNotice = viewModel::clearNotice,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    sharing: Boolean,
    onPickAvatar: () -> Unit,
    onUpdateUsername: (String) -> Unit,
    onLogout: () -> Unit,
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
    val user = (state.user as? LoadState.Ready)?.value
    var showUsername by remember { mutableStateOf(false) }
    var showAvatar by remember { mutableStateOf(false) }
    val downloadUrl = LocalDownloadUrl.current
    Box(Modifier.fillMaxSize().background(AppColors.canvas)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
                    .statusBarsPadding()
                    .padding(20.dp),
            ) {
                Text(stringResource(Res.string.my_profile), style = androidx.compose.material3.MaterialTheme.typography.displaySmall)
                Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clickable(enabled = !state.isUploadingAvatar) { showAvatar = true }) {
                        if (user == null) {
                            Box(Modifier.size(72.dp).clip(CircleShape).background(AppColors.slate300), contentAlignment = Alignment.Center) {
                                if (state.user is LoadState.Loading || state.isUploadingAvatar) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            }
                        } else {
                            val name = displayName(user.username, user.login, user.id)
                            AppAvatar(
                                initials = initialsOf(name),
                                color = avatarColor(user.id),
                                imageUrl = user.avatarUrl,
                                size = 72.dp,
                                live = true,
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .border(2.dp, Color.White, CircleShape)
                                .background(AppColors.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(AppIcons.edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                    Column(Modifier.padding(start = 16.dp).weight(1f).clickable(enabled = user != null) { showUsername = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user?.let { displayName(it.username, it.login, it.id) } ?: stringResource(Res.string.loading),
                                color = AppColors.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                            if (user != null) {
                                Icon(AppIcons.edit, contentDescription = null, tint = AppColors.muted, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                            }
                        }
                        if (user != null) {
                            val handle = user.login.ifBlank { user.username }
                            Text("@$handle", color = AppColors.muted, fontSize = 13.sp)
                        }
                    }
                }
                SharingBanner(sharing, Modifier.padding(top = 16.dp))
            }
            SectionCard(Modifier.padding(top = 12.dp)) {
                MenuRow(icon = AppIcons.badge, label = stringResource(Res.string.change_username), onClick = { if (user != null) showUsername = true })
                if (isWebTarget && downloadUrl.isNotBlank()) {
                    HairlineDivider()
                    MenuRow(icon = AppIcons.download, label = stringResource(Res.string.download_app), onClick = { openExternalUrl(downloadUrl) })
                }
                HairlineDivider()
                MenuRow(icon = AppIcons.logout, label = stringResource(Res.string.log_out), danger = true, onClick = onLogout)
            }
            if (state.user is LoadState.Failed) {
                Text(state.user.message, color = AppColors.danger, modifier = Modifier.padding(20.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
    if (showUsername && user != null) {
        UsernameSheet(
            initial = user.username.ifBlank { displayName(user.username, user.login, user.id) },
            saving = state.isUpdatingUsername,
            onDismiss = { showUsername = false },
            onSave = {
                onUpdateUsername(it)
                showUsername = false
            },
        )
    }
    if (showAvatar) {
        AvatarSheet(
            onDismiss = { showAvatar = false },
            onPick = {
                showAvatar = false
                onPickAvatar()
            },
        )
    }
}

@Composable
private fun noticeText(notice: ProfileNotice): String = when (notice) {
    ProfileNotice.AvatarUpdated -> stringResource(Res.string.avatar_updated)
    ProfileNotice.UsernameUpdated -> stringResource(Res.string.username_updated)
    ProfileNotice.ComingSoon -> stringResource(Res.string.coming_soon)
    is ProfileNotice.Error -> notice.message
}

@Composable
private fun SharingBanner(sharing: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (sharing) AppColors.primarySoft else AppColors.canvas)
            .border(1.dp, if (sharing) AppColors.primaryRing else AppColors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (sharing) AppColors.live else AppColors.offline))
        Column(Modifier.padding(start = 10.dp)) {
            Text(
                stringResource(if (sharing) Res.string.sharing_live_location else Res.string.location_sharing_off),
                color = AppColors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                stringResource(if (sharing) Res.string.friends_can_see_your_location else Res.string.your_location_is_hidden),
                color = AppColors.muted,
                fontSize = 12.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsernameSheet(initial: String, saving: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var username by remember { mutableStateOf(initial) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
            Text(stringResource(Res.string.change_username), color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(stringResource(Res.string.change_username_hint), modifier = Modifier.padding(top = 8.dp, bottom = 20.dp), color = AppColors.muted)
            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.username)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = appTextFieldColors(),
            )
            GradientButton(
                label = stringResource(Res.string.save),
                onClick = { onSave(username) },
                loading = saving,
                enabled = username.isNotBlank(),
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSheet(onDismiss: () -> Unit, onPick: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.camera)) },
                leadingContent = { Icon(AppIcons.camera, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onPick),
            )
            ListItem(
                headlineContent = { Text(stringResource(Res.string.gallery)) },
                leadingContent = { Icon(AppIcons.gallery, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onPick),
            )
        }
    }
}
