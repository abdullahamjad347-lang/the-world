package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDarkGreen,
    onPrimary = Color.White,
    primaryContainer = LightMint,
    onPrimaryContainer = PrimaryDarkGreen,
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = AccentLime,
    onSecondaryContainer = PrimaryDarkGreen,
    tertiary = AccentLime,
    onTertiary = PrimaryDarkGreen,
    background = MainBackground,
    onBackground = MainText,
    surface = CardBackground,
    onSurface = MainText,
    surfaceVariant = Color(0xFFEDF3EF),
    onSurfaceVariant = SecondaryText,
    outline = BorderColor,
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentLime,
    onPrimary = PrimaryDarkGreen,
    primaryContainer = SecondaryGreen,
    onPrimaryContainer = Color.White,
    secondary = AccentLime,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF1E382C),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun KarkunanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KarkunanTypography,
        content = content
    )
}
