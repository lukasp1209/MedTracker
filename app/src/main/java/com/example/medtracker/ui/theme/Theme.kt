package com.example.medtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF256A52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5ECD8),
    onPrimaryContainer = Color(0xFF0A2118),
    secondary = Color(0xFF54634C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E8CB),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF36618E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF0A1D33),
    background = Color(0xFFF6F4EC),
    onBackground = Color(0xFF1A1C18),
    surface = Color(0xFFFFFCF4),
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE0E4D8),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796F),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAD0BC),
    onPrimary = Color(0xFF0D3728),
    primaryContainer = Color(0xFF24513F),
    onPrimaryContainer = Color(0xFFC5ECD8),
    secondary = Color(0xFFBCCCB0),
    onSecondary = Color(0xFF273420),
    secondaryContainer = Color(0xFF3D4B35),
    onSecondaryContainer = Color(0xFFD8E8CB),
    tertiary = Color(0xFFA2C9FA),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF1C496F),
    onTertiaryContainer = Color(0xFFD2E4FF),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE2E3DC),
    surface = Color(0xFF191C18),
    onSurface = Color(0xFFE2E3DC),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9388),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography()

/**
 * Applies the MedTracker Material theme and switches colors based on the selected theme mode.
 */
@Composable
fun MedTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
