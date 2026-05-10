package com.sunflower.timetracker.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deep dark palette
val Background    = Color(0xFF080808)
val Surface       = Color(0xFF111111)
val SurfaceVar    = Color(0xFF1A1A1A)
val CardBg        = Color(0xFF161616)
val Outline       = Color(0xFF2A2A2A)
val Primary       = Color(0xFF7C6AF7)   // muted indigo
val PrimaryVar    = Color(0xFF5B4DDB)
val OnPrimary     = Color(0xFFFFFFFF)
val TextPrimary   = Color(0xFFF0F0F0)
val TextSecondary = Color(0xFF9A9A9A)
val TextTertiary  = Color(0xFF555555)
val AccentGreen   = Color(0xFF1DD1A1)
val AccentRed     = Color(0xFFFF6B6B)

private val DarkColors = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    primaryContainer = PrimaryVar,
    background       = Background,
    onBackground     = TextPrimary,
    surface          = Surface,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVar,
    onSurfaceVariant = TextSecondary,
    outline          = Outline,
    error            = AccentRed,
    onError          = Color.White
)

@Composable
fun TimeTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography  = Typography(),
        content     = content
    )
}