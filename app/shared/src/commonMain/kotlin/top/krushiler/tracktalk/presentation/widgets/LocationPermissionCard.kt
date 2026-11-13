package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.location_needed_body
import tracktalk.app.shared.generated.resources.location_needed_title
import tracktalk.app.shared.generated.resources.open_settings
import top.krushiler.tracktalk.platform.LocationAccess
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.GradientButton

@Composable
fun LocationPermissionCard(
    access: LocationAccess,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.surface)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(Res.string.location_needed_title),
            color = AppColors.text,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.location_needed_body),
            color = AppColors.muted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
        )
        if (access == LocationAccess.Denied) {
            GradientButton(label = stringResource(Res.string.open_settings), onClick = onOpenSettings)
        } else {
            GradientButton(label = stringResource(Res.string.location_needed_title), onClick = onAllow)
        }
    }
}
