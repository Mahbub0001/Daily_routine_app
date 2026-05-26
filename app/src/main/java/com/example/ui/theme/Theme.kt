package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Use dynamically updated color schemes that read from our reactive global theme state
  val colorScheme = if (isAppDarkThemeGlobal) {
    darkColorScheme(
      primary = BentoPrimaryPurple,
      secondary = BentoTextPrimaryBlue,
      tertiary = BeautifulRedAccent,
      background = BentoBg,
      surface = BentoCardWhite,
      onPrimary = Color.Black,
      onSecondary = BentoTextPrimaryPurple,
      onBackground = BentoTextPrimary,
      onSurface = BentoTextPrimary,
    )
  } else {
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
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
