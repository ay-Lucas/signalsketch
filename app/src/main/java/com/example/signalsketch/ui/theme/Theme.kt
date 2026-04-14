package com.example.signalsketch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = YellowPrimaryDark,
    secondary = YellowSecondaryDark,
    tertiary = YellowTertiaryDark,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onPrimary = OnYellow,
    onSecondary = OnYellow,
    onTertiary = OnYellow,
    onBackground = OnDark,
    onSurface = OnDark
)

private val LightColorScheme = lightColorScheme(
    primary = YellowPrimaryLight,
    secondary = YellowSecondaryLight,
    tertiary = YellowTertiaryLight,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onPrimary = OnYellow,
    onSecondary = OnYellow,
    onTertiary = OnYellow,
    onBackground = OnDark,
    onSurface = OnDark
)

@Composable
fun SignalSketchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Force our black + yellow palette; ignore dynamic color
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}