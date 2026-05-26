package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Global theme state of the application, reactive to any recomposing context
var isAppDarkThemeGlobal by mutableStateOf(false)

// Bento Grid Palette with dynamic properties (without @Composable annotations, making them accessible in Canvas and other non-composable scopes while remaining fully reactive)
val BentoBg: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF121214) else Color(0xFFFAF8FF)

val BentoCardWhite: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF1E1E24) else Color(0xFFFFFFFF)

val BentoCardPurple: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF31254F) else Color(0xFFEADDFF)

val BentoCardBlue: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF1D2D44) else Color(0xFFD3E3FD)

val BentoTextPrimary: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

val BentoTextSecondary: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFCAC4D0) else Color(0xFF49454F)

val BentoTextPrimaryPurple: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFEADDFF) else Color(0xFF21005D)

val BentoTextPrimaryBlue: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFD3E3FD) else Color(0xFF041E49)

val BentoPrimaryPurple: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFD0BCFF) else Color(0xFF6750A4)

val BentoBorder: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF49454F) else Color(0xFFCAC4D0)

val BeautifulRedAccent: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFFF2B8B5) else Color(0xFFFFB4AB)

val BeautifulBlueAccent: Color 
    get() = if (isAppDarkThemeGlobal) Color(0xFF8AB4F8) else Color(0xFFB3E5FC)

// Legacy compatibility mapping to Bento colors
val SpaceBlack: Color get() = BentoBg
val DeepViolet: Color get() = BentoCardWhite
val NebulaCard: Color get() = BentoCardWhite
val StellarGlow: Color get() = BentoPrimaryPurple
val CosmicTeal: Color get() = BentoTextPrimaryBlue
val SupernovaPink: Color get() = if (isAppDarkThemeGlobal) Color(0xFFF2B8B5) else Color(0xFFB3261E)
val NebulaGold: Color get() = if (isAppDarkThemeGlobal) Color(0xFFFFB74D) else Color(0xFFE08A00)

// Text and Greys
val MutedSlate: Color get() = BentoTextSecondary
val PureWhite: Color get() = BentoTextPrimary
val TranslucentWhite: Color get() = if (isAppDarkThemeGlobal) Color(0x33FFFFFF) else Color(0x1B000000)

// Standard Palette compatibility fallback
val Purple80: Color get() = StellarGlow
val PurpleGrey80: Color get() = MutedSlate
val Pink80: Color get() = SupernovaPink

val Purple40: Color get() = BentoPrimaryPurple
val PurpleGrey40: Color get() = BentoTextSecondary
val Pink40: Color get() = BeautifulRedAccent
