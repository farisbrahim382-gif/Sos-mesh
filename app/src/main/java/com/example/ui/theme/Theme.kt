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

private val TacticalDarkColorScheme = darkColorScheme(
    primary = EmergencyCrimson,
    onPrimary = Color.White,
    primaryContainer = EmergencyCrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = MeshCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = MeshCyanDim,
    onSecondaryContainer = Color.White,
    tertiary = SignalAmber,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5C3600),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = TacticalDarkBg,
    onBackground = TacticalTextPrimary,
    surface = TacticalSurface,
    onSurface = TacticalTextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TacticalTextSecondary,
    outline = TacticalCardBorder,
    error = EmergencyCrimson,
    onError = Color.White
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = EmergencyCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF006874),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9EEFFD),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF8B5000),
    onTertiary = Color.White,
    background = TacticalLightBg,
    onBackground = TacticalLightTextPrimary,
    surface = TacticalLightSurface,
    onSurface = TacticalLightTextPrimary,
    surfaceVariant = TacticalLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    error = EmergencyCrimson,
    onError = Color.White
)

@Composable
fun SosMeshTheme(
    darkTheme: Boolean = true, // Default to tactical dark OLED mode
    dynamicColor: Boolean = false, // Keep high-contrast tactical identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TacticalDarkColorScheme
        else -> TacticalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SosMeshTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
