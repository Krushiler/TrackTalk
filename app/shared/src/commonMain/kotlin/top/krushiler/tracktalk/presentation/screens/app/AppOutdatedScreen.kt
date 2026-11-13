package top.krushiler.tracktalk.presentation.screens.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.app_outdated_body
import tracktalk.app.shared.generated.resources.app_outdated_title
import tracktalk.app.shared.generated.resources.download_update
import top.krushiler.tracktalk.platform.openExternalUrl
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons
import top.krushiler.tracktalk.presentation.theme.GradientButton
import top.krushiler.tracktalk.presentation.theme.LocalDownloadUrl

@Composable
fun AppOutdatedScreen() {
    val downloadUrl = LocalDownloadUrl.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.loginGradient)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.systemUpdate, contentDescription = null, tint = AppColors.primary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(Res.string.app_outdated_title),
            modifier = Modifier.padding(top = 32.dp),
            style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.app_outdated_body),
            modifier = Modifier.padding(top = 12.dp, bottom = 40.dp),
            color = AppColors.muted,
            textAlign = TextAlign.Center,
        )
        GradientButton(
            label = stringResource(Res.string.download_update),
            onClick = { if (downloadUrl.isNotBlank()) openExternalUrl(downloadUrl) },
            enabled = downloadUrl.isNotBlank(),
        )
    }
}
