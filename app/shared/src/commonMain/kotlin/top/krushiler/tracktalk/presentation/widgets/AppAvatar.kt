package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import top.krushiler.tracktalk.presentation.theme.AppColors

@Composable
fun AppAvatar(
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    imageUrl: String? = null,
    online: Boolean? = null,
    live: Boolean = false,
) {
    val ring = when {
        live -> AppColors.live
        online == true -> AppColors.online
        else -> AppColors.slate300
    }
    Box(modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(if (live || online == true) 0.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (live || online == true) {
                        Modifier.border(3.dp, ring.copy(alpha = 0.45f), CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = rememberRemoteBitmap(imageUrl)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                )
            }
        }
        if (live) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(1.dp, 1.dp)
                    .size(size * 0.28f)
                    .border(2.dp, Color.White, CircleShape)
                    .background(AppColors.live, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(Color.White, CircleShape),
                )
            }
        } else if (online == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.26f)
                    .border(2.dp, Color.White, CircleShape)
                    .background(AppColors.online, CircleShape),
            )
        }
    }
}

@Composable
fun rememberRemoteBitmap(url: String?): ImageBitmap? {
    val http = koinInject<HttpClient>()
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        val target = url?.trim().orEmpty()
        bitmap = if (target.isEmpty()) null else BitmapCache.load(http, target)
    }
    return bitmap
}
