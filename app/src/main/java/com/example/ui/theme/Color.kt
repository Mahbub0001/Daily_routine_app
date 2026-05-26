package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Bento Grid Palette
val BentoBg = Color(0xFFFAF8FF)          // Warm light background
val BentoCardWhite = Color(0xFFFFFFFF)   // Main white bento boxes
val BentoCardPurple = Color(0xFFEADDFF)  // Main accent purple box
val BentoCardBlue = Color(0xFFD3E3FD)    // Light blue goal box
val BentoTextPrimary = Color(0xFF1C1B1F)  // Dark body text
val BentoTextSecondary = Color(0xFF49454F)// Medium dark description text
val BentoTextPrimaryPurple = Color(0xFF21005D) // Deep purple highlight text
val BentoTextPrimaryBlue = Color(0xFF041E49)  // Deep blue goal text
val BentoPrimaryPurple = Color(0xFF6750A4)    // Brand purple accent
val BentoBorder = Color(0xFFCAC4D0)      // Subtle border
val BeautifulRedAccent = Color(0xFFFFB4AB) // Warning or active highlights
val BeautifulBlueAccent = Color(0xFFB3E5FC) // Alternate soft highlights

// Legacy compatibility mapping to Bento colors
val SpaceBlack = BentoBg
val DeepViolet = BentoCardWhite
val NebulaCard = BentoCardWhite // Match bento card standard white
val StellarGlow = BentoPrimaryPurple
val CosmicTeal = BentoTextPrimaryBlue
val SupernovaPink = Color(0xFFB3261E) // High contrast dark red
val NebulaGold = Color(0xFFE08A00) // Beautiful amber contrast

// Text and Greys
val MutedSlate = BentoTextSecondary
val PureWhite = BentoTextPrimary
val TranslucentWhite = Color(0x1B000000)

// Standard Palette compatibility fallback
val Purple80 = StellarGlow
val PurpleGrey80 = MutedSlate
val Pink80 = SupernovaPink

val Purple40 = BentoPrimaryPurple
val PurpleGrey40 = BentoTextSecondary
val Pink40 = BeautifulRedAccent


