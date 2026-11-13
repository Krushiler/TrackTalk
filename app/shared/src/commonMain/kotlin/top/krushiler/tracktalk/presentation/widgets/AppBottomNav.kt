package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.nav_friends
import tracktalk.app.shared.generated.resources.nav_map
import tracktalk.app.shared.generated.resources.nav_profile
import top.krushiler.tracktalk.presentation.theme.AppColors
import top.krushiler.tracktalk.presentation.theme.AppIcons

enum class HomeTab(val label: @Composable () -> String, val icon: ImageVector, val activeIcon: ImageVector) {
    Map({ stringResource(Res.string.nav_map) }, AppIcons.mapOutlined, AppIcons.map),
    Friends({ stringResource(Res.string.nav_friends) }, AppIcons.peopleOutlined, AppIcons.people),
    Profile({ stringResource(Res.string.nav_profile) }, AppIcons.personOutlined, AppIcons.person),
}

@Composable
fun AppBottomNav(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(AppColors.surface.copy(alpha = 0.96f))) {
        HorizontalDivider(color = AppColors.text.copy(alpha = 0.06f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                val color = if (active) AppColors.primary else AppColors.slate400
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = null, indication = null) { onSelect(tab) }
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (active) tab.activeIcon else tab.icon,
                        contentDescription = tab.label(),
                        tint = color,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label(),
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
        }
    }
}
