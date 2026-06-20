package com.mio.voice.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mio Voice 黄绿主色调（贴近概念图的明亮黄绿）
private val MioGreen = Color(0xFFA4C72A)
private val MioGreenDark = Color(0xFF89A91E)
private val MioGreenLight = Color(0xFFEDF3D2)

private val LightColors = lightColorScheme(
    primary = MioGreen,
    onPrimary = Color(0xFF26310A),
    primaryContainer = MioGreenLight,
    onPrimaryContainer = Color(0xFF313F00),
    secondary = MioGreenDark,
    onSecondary = Color.White,
    secondaryContainer = MioGreenLight,
    onSecondaryContainer = Color(0xFF313F00),
    tertiary = Color(0xFF5C6146),
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFEAEBDE),
    onSurfaceVariant = Color(0xFF45483A),
    surfaceContainer = Color(0xFFF2F4E8),
    outline = Color(0xFFDADCCB),
    outlineVariant = Color(0xFFE8EAD8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = MioGreen,
    onPrimary = Color(0xFF1F2600),
    primaryContainer = MioGreenDark,
    onPrimaryContainer = MioGreenLight,
    secondary = Color(0xFFC4CE8E),
    onSecondary = Color(0xFF2C3600),
    secondaryContainer = Color(0xFF424A1B),
    onSecondaryContainer = MioGreenLight,
    tertiary = Color(0xFFC4C9A8),
    background = Color(0xFF12140E),
    onBackground = Color(0xFFE3E3D7),
    surface = Color(0xFF12140E),
    onSurface = Color(0xFFE3E3D7),
    surfaceVariant = Color(0xFF47483B),
    onSurfaceVariant = Color(0xFFC8C8B6),
    surfaceContainer = Color(0xFF1E2018),
    outline = Color(0xFF929382),
    outlineVariant = Color(0xFF47483B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MioVoiceTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
