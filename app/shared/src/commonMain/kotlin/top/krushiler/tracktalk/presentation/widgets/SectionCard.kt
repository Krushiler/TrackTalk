package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface),
    ) {
        content()
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.weight(1f),
            color = AppColors.slate400,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
        )
        if (count != null) {
            Text(
                text = count.toString(),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.hairline)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = AppColors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    value: String? = null,
) {
    val iconColor = if (danger) AppColors.danger else AppColors.muted
    val labelColor = if (danger) AppColors.danger else AppColors.text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (danger) AppColors.dangerSoft else AppColors.hairline),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            color = labelColor,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
        )
        when {
            trailing != null -> trailing()
            value != null -> {
                Text(value, color = AppColors.slate400, fontSize = 14.sp)
                Icon(AppIcons.chevronRight, contentDescription = null, tint = AppColors.slate300, modifier = Modifier.size(18.dp))
            }
            !danger -> Icon(
                AppIcons.chevronRight,
                contentDescription = null,
                tint = AppColors.slate300,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun HairlineDivider(startIndent: androidx.compose.ui.unit.Dp = 70.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent),
        thickness = 1.dp,
        color = AppColors.hairline,
    )
}

@Composable
fun EmptyHint(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        color = AppColors.muted,
        fontSize = 13.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.slate300),
    )
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
    trailing: @Composable (() -> Unit)? = null,
    below: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
            )
            trailing?.invoke()
        }
        below?.invoke()
    }
}
