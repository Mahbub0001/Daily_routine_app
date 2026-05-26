package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color


private val BentoColorScheme =
  lightColorScheme(
    primary = BentoPrimaryPurple,
    secondary = BentoTextPrimaryBlue,
    tertiary = BeautifulRedAccent,
    background = BentoBg,
    surface = BentoCardWhite,
    onPrimary = Color.White,
    onSecondary = BentoTextPrimaryPurple,
    onBackground = BentoTextPrimary,
    onSurface = BentoTextPrimary,
  )

private val LightColorScheme = BentoColorScheme
private val DarkColorScheme = BentoColorScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Use the Bento light theme
  dynamicColor: Boolean = false, // Force custom palette instead of system colors
  content: @Composable () -> Unit,
) {
  val colorScheme = BentoColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
