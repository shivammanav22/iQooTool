package com.perfmode.iqoo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat // Import WindowCompat

// Define Gothic/Crow-inspired Color Scheme
private val GothicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB00020), // Dark Red/Maroon for subtle primary accents (like the title text in the poster)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF212121), // Very dark grey for containers
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBB86FC), // A muted purple for secondary, if needed (can be adjusted)
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3700B3), // Darker muted purple
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF03DAC5), // Teal for tertiary, if needed (can be adjusted)
    onTertiary = Color.Black,
    background = Color(0xFF000000), // Pure Black background
    onBackground = Color.White,
    surface = Color(0xFF121212), // Dark grey for cards/elements
    onSurface = Color.White,
    surfaceVariant = Color(0xFF212121), // Slightly lighter dark grey for card backgrounds
    onSurfaceVariant = Color.White,
    error = Color(0xFFCF6679), // Standard error red
    onError = Color.Black,
    outline = Color(0xFF424242) // Subtle dark grey outline
)

// Since the app is forced to dark mode, this LightColorScheme will not be used.
// It's kept for completeness but can be removed if strictly dark mode is desired.
private val GothicLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White
)


@Composable
fun IqooTweaksTheme(
    darkTheme: Boolean = true, // Forced to always true for dark mode
    dynamicColor: Boolean = false, // Set to false to use our custom color schemes
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> GothicDarkColorScheme // Use our custom dark scheme
        else -> GothicLightColorScheme // This branch will not be hit due to darkTheme = true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color to match the surface color for a seamless dark look
            window.statusBarColor = colorScheme.surface.toArgb()
            // Adjust status bar icons to be light on dark background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // False for dark status bar icons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming Typography is defined elsewhere in your theme package
        content = content
    )
}
