package top.krushiler.tracktalk.presentation.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
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
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.accept
import tracktalk.app.shared.generated.resources.add_friend
import tracktalk.app.shared.generated.resources.add_friend_hint
import tracktalk.app.shared.generated.resources.cancel
import tracktalk.app.shared.generated.resources.decline
import tracktalk.app.shared.generated.resources.friend_login
import tracktalk.app.shared.generated.resources.send_request
import top.krushiler.tracktalk.model.Friend
import top.krushiler.tracktalk.presentation.LoadState
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.GradientButton
import top.krushiler.tracktalk.presentation.theme.appTextFieldColors
import top.krushiler.tracktalk.presentation.util.avatarColor
import top.krushiler.tracktalk.presentation.util.displayName
import top.krushiler.tracktalk.presentation.util.initialsOf
import top.krushiler.tracktalk.presentation.widgets.AppAvatar
import top.krushiler.tracktalk.presentation.widgets.EmptyHint
import top.krushiler.tracktalk.presentation.widgets.HairlineDivider
import top.krushiler.tracktalk.presentation.widgets.SectionCard
import top.krushiler.tracktalk.presentation.widgets.SectionHeader
import top.krushiler.tracktalk.presentation.widgets.SheetHandle

@Composable
internal fun AddFriendButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            AppIcons.personAdd,
            contentDescription = stringResource(Res.string.add_friend),
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun FriendPeopleSection(
    title: String,
    state: LoadState<List<Friend>>,
    empty: String,
    people: List<Friend>?,
    tile: @Composable (Friend) -> Unit,
) {
    SectionHeader(title = title, count = people?.size)
    when {
        state is LoadState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.primary)
        }
        state is LoadState.Failed -> Text(
            state.message,
            color = AppColors.danger,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 13.sp,
        )
        people.isNullOrEmpty() -> EmptyHint(empty, Modifier.padding(horizontal = 16.dp))
        else -> SectionCard {
            people.forEachIndexed { index, friend ->
                tile(friend)
                if (index < people.lastIndex) HairlineDivider(80.dp)
            }
        }
    }
}

@Composable
internal fun UserRow(friend: Friend) {
    val name = displayName(friend.username, friend.login, friend.id)
    val handle = friend.login.takeIf { it.isNotBlank() && it != name }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppAvatar(initials = initialsOf(name), color = avatarColor(friend.id), imageUrl = friend.avatarUrl, size = 48.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(name, color = AppColors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (handle != null) Text("@$handle", color = AppColors.muted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun IncomingTile(friend: Friend, busy: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    Column(Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        UserRow(friend)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.decline), fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.accept), color = AppColors.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun OutgoingTile(friend: Friend, busy: Boolean, onCancel: () -> Unit) {
    Row(
        Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { UserRow(friend) }
        TextButton(onClick = onCancel, enabled = !busy) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(stringResource(Res.string.cancel), color = AppColors.danger, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFriendSheet(sending: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var login by remember { mutableStateOf("") }
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
            Text(stringResource(Res.string.add_friend), color = AppColors.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(
                stringResource(Res.string.add_friend_hint),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                color = AppColors.muted,
                fontSize = 14.sp,
            )
            TextField(
                value = login,
                onValueChange = { login = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.friend_login)) },
                leadingIcon = { Icon(AppIcons.alternateEmail, contentDescription = null, tint = AppColors.slate400) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = appTextFieldColors(),
            )
            Spacer(Modifier.height(20.dp))
            GradientButton(
                label = stringResource(Res.string.send_request),
                onClick = { onSend(login) },
                loading = sending,
                enabled = login.isNotBlank(),
            )
        }
    }
}
