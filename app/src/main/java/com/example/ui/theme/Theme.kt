package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StealthBlackColorScheme = darkColorScheme(
    primary = TacticalGreen,
    onPrimary = Color.Black,
    primaryContainer = DarkGreyCard,
    onPrimaryContainer = TacticalGreen,
    secondary = TacticalGreenDim,
    onSecondary = Color.Black,
    secondaryContainer = DarkGreyCard,
    onSecondaryContainer = TacticalGreen,
    tertiary = Color(0xFF717682),
    onTertiary = Color.Black,
    background = StealthBlack,
    onBackground = TextWhite,
    surface = DarkGreySurface,
    onSurface = TextWhite,
    surfaceVariant = DarkGreyCard,
    onSurfaceVariant = TextMuted,
    outline = DarkGreyBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StealthBlackColorScheme,
        typography = Typography,
        content = content
    )
}
