package top.krushiler.tracktalk.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val bg = Color(0xFFF4F5F9)
    val canvas = Color(0xFFF8FAFC)
    val surface = Color(0xFFFFFFFF)
    val primary = Color(0xFF2563EB)
    val primaryDark = Color(0xFF1D4ED8)
    val primarySoft = Color(0xFFEFF6FF)
    val primaryRing = Color(0xFFBFDBFE)
    val purple = Color(0xFF7C3AED)
    val online = Color(0xFF22C55E)
    val offline = Color(0xFF94A3B8)
    val live = Color(0xFF3B82F6)
    val text = Color(0xFF0F172A)
    val muted = Color(0xFF64748B)
    val border = Color(0xFFE2E8F0)
    val hairline = Color(0xFFF1F5F9)
    val inputFill = Color(0xFFF1F5F9)
    val danger = Color(0xFFEF4444)
    val dangerSoft = Color(0xFFFEF2F2)
    val warning = Color(0xFFF59E0B)
    val successSoft = Color(0xFFF0FDF4)
    val slate400 = Color(0xFF94A3B8)
    val slate300 = Color(0xFFCBD5E1)

    val loginGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFEEF2FF), Color(0xFFF0F9FF), Color(0xFFF5F3FF)),
    )

    val primaryGradient = Brush.linearGradient(
        colors = listOf(primary, purple),
    )

    val avatarPalette = listOf(
        Color(0xFF8B5CF6),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFF2563EB),
        Color(0xFFEF4444),
        Color(0xFF06B6D4),
        Color(0xFF7C3AED),
    )
}
