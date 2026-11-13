package top.krushiler.tracktalk.presentation.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import tracktalk.app.shared.generated.resources.Res
import tracktalk.app.shared.generated.resources.dm_sans_bold
import tracktalk.app.shared.generated.resources.dm_sans_semibold
import tracktalk.app.shared.generated.resources.inter_regular

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.primary,
            onPrimary = Color.White,
            primaryContainer = AppColors.primarySoft,
            onPrimaryContainer = AppColors.primaryDark,
            secondary = AppColors.purple,
            onSecondary = Color.White,
            background = AppColors.canvas,
            onBackground = AppColors.text,
            surface = AppColors.surface,
            onSurface = AppColors.text,
            surfaceVariant = AppColors.hairline,
            onSurfaceVariant = AppColors.muted,
            surfaceContainerHighest = AppColors.inputFill,
            error = AppColors.danger,
            onError = Color.White,
            errorContainer = AppColors.dangerSoft,
            onErrorContainer = AppColors.danger,
            outline = AppColors.border,
            outlineVariant = AppColors.hairline,
            inverseSurface = AppColors.text,
            inverseOnSurface = Color.White,
        ),
        typography = appTypography(),
        shapes = AppShapes,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.canvas,
            contentColor = AppColors.text,
        ) {
            content()
        }
    }
}

@Composable
private fun appTypography(): Typography {
    val dmSans = FontFamily(
        Font(Res.font.dm_sans_semibold, FontWeight.SemiBold),
        Font(Res.font.dm_sans_bold, FontWeight.Bold),
    )
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
    )
    return Typography(
        displaySmall = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp,
            color = AppColors.text,
        ),
        headlineMedium = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AppColors.text,
        ),
        headlineSmall = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = AppColors.text,
        ),
        titleLarge = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = AppColors.text,
        ),
        titleMedium = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = AppColors.text,
        ),
        titleSmall = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = AppColors.text,
        ),
        bodyLarge = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = AppColors.text,
        ),
        bodyMedium = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = AppColors.muted,
        ),
        bodySmall = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = AppColors.muted,
        ),
        labelLarge = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.White,
        ),
        labelMedium = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = AppColors.muted,
        ),
        labelSmall = TextStyle(
            fontFamily = dmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = AppColors.slate400,
        ),
    )
}

@Composable
fun appTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = AppColors.inputFill,
    unfocusedContainerColor = AppColors.inputFill,
    disabledContainerColor = AppColors.inputFill,
    errorContainerColor = AppColors.inputFill,
    focusedIndicatorColor = AppColors.primary,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = AppColors.danger,
    cursorColor = AppColors.primary,
    errorCursorColor = AppColors.danger,
    focusedTextColor = AppColors.text,
    unfocusedTextColor = AppColors.text,
    focusedLabelColor = AppColors.muted,
    unfocusedLabelColor = AppColors.muted,
    focusedPlaceholderColor = AppColors.slate400,
    unfocusedPlaceholderColor = AppColors.slate400,
)
