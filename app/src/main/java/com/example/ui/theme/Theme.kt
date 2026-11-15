package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val HallLightColorScheme =
  lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = PureWarmWhite,
    primaryContainer = EmeraldAvailable,
    onPrimaryContainer = PureWarmWhite,
    secondary = GoldAccent,
    onSecondary = EmeraldDeep,
    secondaryContainer = SoftGoldTint,
    onSecondaryContainer = EmeraldDeep,
    tertiary = PendingYellow,
    onTertiary = EmeraldDeep,
    background = IvoryCream,
    onBackground = CharcoalText,
    surface = PureWarmWhite,
    onSurface = CharcoalText,
    surfaceVariant = Color(0xFFF2ECE0),
    onSurfaceVariant = MutedSageText,
    outline = InputBorder,
    error = BookedRed,
    onError = PureWarmWhite,
  )

private val HallShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = HallLightColorScheme,
    typography = Typography,
    shapes = HallShapes,
    content = content,
  )
}
