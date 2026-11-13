package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import top.krushiler.tracktalk.model.FriendLocation
import top.krushiler.tracktalk.presentation.util.avatarColor
import top.krushiler.tracktalk.presentation.util.initialsOf
import top.krushiler.tracktalk.presentation.util.personName

@Composable
fun FriendPin(
    friend: FriendLocation,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val user = friend.user
    val name = personName(user.username, user.login).ifEmpty { "?" }
    AppAvatar(
        initials = initialsOf(name),
        color = avatarColor(user.id),
        imageUrl = user.avatarUrl,
        size = 44.dp,
        modifier = modifier
            .graphicsLayer {
                val scale = if (selected) 1.12f else 1f
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (selected) 10.dp else 6.dp, CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .size(44.dp),
    )
}
