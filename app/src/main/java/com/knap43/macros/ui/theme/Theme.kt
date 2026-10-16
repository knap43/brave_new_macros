package com.knap43.macros.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Amber = Color(0xFFE1A34F)

private val Colors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2B1A00),
    primaryContainer = Color(0xFF4A3412),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFD7C3AE),
    onSecondary = Color(0xFF3B2E1F),
    secondaryContainer = Color(0xFF3A3128),
    onSecondaryContainer = Color(0xFFF2DFC9),
    tertiary = Color(0xFFB8CBA0),
    background = Color(0xFF141210),
    onBackground = Color(0xFFEAE1D9),
    surface = Color(0xFF141210),
    onSurface = Color(0xFFEAE1D9),
    surfaceVariant = Color(0xFF2A2520),
    onSurfaceVariant = Color(0xFFCFC4B8),
    surfaceContainerLowest = Color(0xFF0F0D0B),
    surfaceContainerLow = Color(0xFF1C1915),
    surfaceContainer = Color(0xFF201C18),
    surfaceContainerHigh = Color(0xFF2A2621),
    surfaceContainerHighest = Color(0xFF35302A),
    outline = Color(0xFF988F84),
    outlineVariant = Color(0xFF4C463E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Titles are set in serif, as in the other apps; the chrome stays sans. */
val ProseFamily = FontFamily.Serif

private val Base = Typography()

private val Type = Base.copy(
    displaySmall = Base.displaySmall.copy(fontFamily = ProseFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = ProseFamily),
    headlineSmall = Base.headlineSmall.copy(fontFamily = ProseFamily, fontWeight = FontWeight.Medium),
    titleLarge = Base.titleLarge.copy(fontFamily = ProseFamily),
)


/** The amber scheme, or the wallpaper's (always dark) when "Use system colors" is on. */
@Composable
private fun colorScheme(amber: ColorScheme): ColorScheme {
    val context = LocalContext.current
    val systemColors by Appearance.systemColors(context).collectAsState()
    return if (systemColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else amber
}

@Composable
fun MacrosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme(Colors), typography = Type, content = content)
}
