package com.example.imagen.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.imagen.ui.screens.HordeImagenStorage

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8F169),
    onPrimary = Color(0xFF1A2200),
    primaryContainer = Color(0xFF3A4A1E),
    onPrimaryContainer = Color(0xFFE4FFA8),
    secondary = Color(0xFFC4CBAF),
    onSecondary = Color(0xFF2C341F),
    secondaryContainer = Color(0xFF3E4626),
    onSecondaryContainer = Color(0xFFE0E8CA),
    tertiary = Color(0xFFA0D0C0),
    background = Color(0xFF0F110B),
    onBackground = Color(0xFFE5E7DA),
    surface = Color(0xFF0F110B),
    onSurface = Color(0xFFE5E7DA),
    surfaceVariant = Color(0xFF2B311D),
    onSurfaceVariant = Color(0xFFC9CCBA),
    surfaceContainerLow = Color(0xFF171A10),
    surfaceContainer = Color(0xFF1C2015),
    surfaceContainerHigh = Color(0xFF232818),
    surfaceContainerHighest = Color(0xFF2B311D),
    outline = Color(0xFF939786),
    outlineVariant = Color(0xFF3C4230),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C6600),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F58E),
    onPrimaryContainer = Color(0xFF152000),
    secondary = Color(0xFF57624A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7C4),
    onSecondaryContainer = Color(0xFF151E0B),
    tertiary = Color(0xFF3A6A5A),
    background = Color(0xFFFBFDF2),
    onBackground = Color(0xFF1A1C13),
    surface = Color(0xFFFBFDF2),
    onSurface = Color(0xFF1A1C13),
    surfaceVariant = Color(0xFFE1E4D0),
    onSurfaceVariant = Color(0xFF464838),
    surfaceContainerLow = Color(0xFFF4F7E9),
    surfaceContainer = Color(0xFFEFF2E3),
    surfaceContainerHigh = Color(0xFFE9ECDD),
    surfaceContainerHighest = Color(0xFFE3E7D7),
    outline = Color(0xFF767869),
    outlineVariant = Color(0xFFC6C8B8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun rememberIsDarkMode(): Boolean {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("horde_imagen_settings", Context.MODE_PRIVATE)

    var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "dark_mode") {
                isDark = prefs.getBoolean("dark_mode", true)
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return isDark
}

@Composable
fun HordeImagenTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDark = rememberIsDarkMode()

    LaunchedEffect(Unit) {
        HordeImagenStorage.autoEmptyBinIfNeeded(context)
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}