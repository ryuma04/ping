package com.inception.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = InceptionGreen,
    onPrimary = Color.Black,
    primaryContainer = InceptionGreenDark,
    onPrimaryContainer = InceptionGreenLight,
    secondary = InceptionBlue,
    onSecondary = Color.Black,
    secondaryContainer = InceptionBlueDark,
    onSecondaryContainer = InceptionBlueLight,
    tertiary = InceptionOrange,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFF5F5F5),
    surface = DarkSurface,
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF9AA69A),
    outline = DarkOutline,
    error = InceptionRed,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF248A3D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F1D8),
    onPrimaryContainer = Color(0xFF0A3212),
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E9FF),
    onSecondaryContainer = Color(0xFF002C5C),
    tertiary = InceptionOrange,
    onTertiary = Color.Black,
    background = LightBackground,
    onBackground = Color(0xFF131A13),
    surface = LightSurface,
    onSurface = Color(0xFF131A13),
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    error = InceptionRed,
    onError = Color.White
)

@Composable
fun InceptionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
