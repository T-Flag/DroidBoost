package fr.tflag.droidboost.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Material You colors taken from the wallpaper, as on every Pixel app. */
@Composable
fun DroidBoostTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

// Semantic colors for states (good / warning / critical), consistent with the dynamic scheme.
val ColorScheme.ok: Color get() = tertiary

val ColorScheme.warning: Color
    @Composable
    get() = if (isSystemInDarkTheme()) Color(0xFFFFB95C) else Color(0xFF8A5100)

val ColorScheme.critical: Color get() = error
