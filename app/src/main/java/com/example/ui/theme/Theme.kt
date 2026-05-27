package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SleekDarkPrimary,
    secondary = SleekDarkOnSurfaceVariant,
    tertiary = SleekDarkMuted,
    background = SleekDarkBg,
    surface = SleekDarkSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onPrimary = SleekDarkOnPrimary,
    onSecondary = SleekDarkOnPrimary,
    onBackground = SleekDarkOnSurfaceVariant,
    onSurface = SleekDarkOnSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant,
    onError = Color.White,
    error = SleekErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    secondary = SleekMutedText,
    tertiary = SleekDashedBorder,
    background = SleekBackground,
    surface = SleekSurface,
    surfaceVariant = SleekSurfaceVariant,
    onPrimary = SleekOnPrimary,
    onSecondary = SleekOnPrimary,
    onBackground = SleekOnBackground,
    onSurface = SleekOnBackground,
    onSurfaceVariant = SleekOnSurfaceVariant,
    outline = SleekOutline,
    error = SleekErrorRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color support to respect Android 12+ wallpaper coloring if preferred,
    // but default to false to ensure our custom Eco-Sage look is perfectly showcased
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
