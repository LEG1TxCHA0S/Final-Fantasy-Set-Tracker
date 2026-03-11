package com.chaos.finalfantasysettracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = CrystalBlue,
    onPrimary = CrystalNight,
    primaryContainer = AbyssBlue,
    onPrimaryContainer = MoonlitBlue,
    secondary = MistSilver,
    onSecondary = CrystalNight,
    secondaryContainer = SlateSurface,
    onSecondaryContainer = MistSilver,
    tertiary = SoftGold,
    onTertiary = CrystalNight,
    tertiaryContainer = ColorTokens.tertiaryContainer,
    onTertiaryContainer = SoftGold,
    background = CrystalNight,
    onBackground = MistSilver,
    surface = MidnightBlue,
    onSurface = MistSilver,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = MoonlitBlue,
    error = EmberRose,
    onError = CrystalNight,
    errorContainer = ColorTokens.errorContainer,
    onErrorContainer = MistSilver
)

private object ColorTokens {
    val tertiaryContainer = SoftGold.copy(alpha = 0.16f)
    val errorContainer = EmberRose.copy(alpha = 0.20f)
}

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FinalFantasySetTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

val ElevatedCardColors
    @Composable
    get() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface
    )
