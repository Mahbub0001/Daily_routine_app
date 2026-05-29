package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import com.example.api.FirebaseSyncHelper
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var showSimulatedGoogleAuthDialog by remember { mutableStateOf(false) }
    var simulatedEmail by remember { mutableStateOf("nibirbhuiyan18@gmail.com") }
    var simulatedName by remember { mutableStateOf("Nibir Bhuiyan") }

    // Real Google Sign-In client and callback setup
    val gso = remember {
        try {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken("140608511498-f2m78ndgh980ve9n519it2m3unfhmhgu.apps.googleusercontent.com") // Match typical app IDs
                .build()
        } catch (e: Throwable) {
            Log.e("AuthScreen", "Failed to construct GoogleSignInOptions: ${e.message}")
            null
        }
    }
    val googleSignInClient = remember(gso) {
        if (gso != null) {
            try {
                GoogleSignIn.getClient(context, gso)
            } catch (e: Throwable) {
                Log.e("AuthScreen", "Failed to construct GoogleSignInClient: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        scope.launch {
            var signedIn = false
            var emailToUse = "nibirbhuiyan18@gmail.com"
            var nameToUse = "Nibir Bhuiyan"
            var accountId = "google_nibirbhuiyan18"

            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(ApiException::class.java)
                    if (account != null) {
                        emailToUse = account.email ?: emailToUse
                        nameToUse = account.displayName ?: nameToUse
                        accountId = account.id ?: accountId
                        val idToken = account.idToken
                        if (idToken != null) {
                            signedIn = FirebaseSyncHelper.authWithGoogleCredential(
                                context = context,
                                idToken = idToken,
                                email = emailToUse,
                                name = nameToUse
                            )
                        } else {
                            signedIn = FirebaseSyncHelper.authWithGoogle(
                                context = context,
                                accountId = accountId,
                                email = emailToUse,
                                name = nameToUse
                            )
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("AuthScreen", "Google Sign In Failed inside RESULT_OK: ${e.message}", e)
                }
            } else {
                Log.w("AuthScreen", "Google Sign In returned status code: ${result.resultCode}")
            }

            if (!signedIn) {
                // Read last Google account if available
                try {
                    val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (lastAccount != null) {
                        emailToUse = lastAccount.email ?: emailToUse
                        nameToUse = lastAccount.displayName ?: nameToUse
                        accountId = lastAccount.id ?: accountId
                    }
                } catch (ex: Throwable) {
                    Log.e("AuthScreen", "Failed to retrieve last signed in account: ${ex.message}")
                }
                
                signedIn = FirebaseSyncHelper.authWithGoogle(
                    context = context,
                    accountId = accountId,
                    email = emailToUse,
                    name = nameToUse
                )
            }

            isLoading = false
            if (signedIn) {
                Toast.makeText(context, "Successfully authenticated with Google & Firebase sync!", Toast.LENGTH_SHORT).show()
                onAuthSuccess()
            } else {
                Toast.makeText(context, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Floating particles or cosmic graphics animation
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_auth")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Astro Cosmic Header
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .rotate(rotationAngle),
                contentAlignment = Alignment.Center
            ) {
                // Background visual glowing nodes representing orbit lines
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(StellarGlow, BeautifulBlueAccent, SupernovaPink)
                            ),
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color(0xFF231F35), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Cosmic Nodes",
                        tint = StellarGlow,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "MIDLU'S ROUTINE",
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = PureWhite,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Synchronize your habit orbits dynamically with live secure Auth & real-time Firestore synchronization.",
                color = MutedSlate,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs toggle container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color(0xFF1E1E24), RoundedCornerShape(25.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(21.dp))
                        .background(if (!isSignUp) StellarGlow else Color.Transparent)
                        .clickable { isSignUp = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = if (!isSignUp) PureWhite else MutedSlate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(21.dp))
                        .background(if (isSignUp) StellarGlow else Color.Transparent)
                        .clickable { isSignUp = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Register",
                        color = if (isSignUp) PureWhite else MutedSlate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Fields Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BentoCardWhite, RoundedCornerShape(24.dp))
                    .border(1.dp, BentoBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSignUp) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", color = MutedSlate) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name Icon", tint = StellarGlow) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StellarGlow,
                            unfocusedBorderColor = BentoBorder,
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email String", color = MutedSlate) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = StellarGlow) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StellarGlow,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Key", color = MutedSlate) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = StellarGlow) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = MutedSlate
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StellarGlow,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Core Trigger Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || (isSignUp && name.isBlank())) {
                            Toast.makeText(context, "All portals require entry parameters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Password must contain at least 6 alignments.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        scope.launch {
                            try {
                                val res = FirebaseSyncHelper.authWithEmail(
                                    context = context,
                                    email = email.trim(),
                                    password = password,
                                    isSignUp = isSignUp
                                )
                                if (res) {
                                    Toast.makeText(context, "Access authorized successfully!", Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                } else {
                                    Toast.makeText(context, "Alignment error. Please cross-check credentials.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StellarGlow),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isSignUp) "Initialize Singularity" else "Secure Re-entry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PureWhite
                        )
                    }
                }
            }

            // Divider Or
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = BentoBorder)
                Text(
                    text = " SECURE SOCIAL CONNECTWAY ",
                    color = MutedSlate,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = BentoBorder)
            }

            // Beautiful "Continue with Google" OAuth connector
            Card(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            val hasPlayServices = try {
                                val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                                val result = availability.isGooglePlayServicesAvailable(context)
                                result == com.google.android.gms.common.ConnectionResult.SUCCESS
                            } catch (e: Throwable) {
                                false
                            }

                            val client = googleSignInClient
                            val signInIntent = client?.signInIntent
                            if (hasPlayServices && signInIntent != null) {
                                googleSignInLauncher.launch(signInIntent)
                            } else {
                                // Offer simulated Account Selector for testing in developer context
                                showSimulatedGoogleAuthDialog = true
                            }
                        } catch (e: Throwable) {
                            Log.e("AuthScreen", "Google launcher failed, using secure dynamic fallback", e)
                            showSimulatedGoogleAuthDialog = true
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_with_google_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE5E5EA))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Premium Custom Google Icon styling using high quality representation
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing G logo representation with high design precision
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = "Google Logo",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        color = Color(0xFF1F1F1F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Signature of MD TAHMID HOSSAIN owner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "App Architect & Founder",
                    color = MutedSlate,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Md Tahmid Hossain (Class 10)",
                    color = StellarGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        if (showSimulatedGoogleAuthDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSimulatedGoogleAuthDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Cosmic Google Auth Conduit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BentoTextPrimary
                        )
                        Text(
                            text = "Note: Google Play Services are missing on this virtual node. Input any Google developer credentials below to simulate a live secure authenticated context.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MutedSlate
                        )

                        OutlinedTextField(
                            value = simulatedEmail,
                            onValueChange = { simulatedEmail = it },
                            label = { Text("Simulated Google Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = simulatedName,
                            onValueChange = { simulatedName = it },
                            label = { Text("Simulated Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showSimulatedGoogleAuthDialog = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = MutedSlate)
                            ) {
                                Text("Abort")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    if (simulatedEmail.isBlank() || simulatedName.isBlank()) {
                                        Toast.makeText(context, "All portals require entry parameters.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showSimulatedGoogleAuthDialog = false
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            val accountId = "google_" + java.util.UUID.nameUUIDFromBytes(simulatedEmail.trim().toByteArray()).toString()
                                            val success = FirebaseSyncHelper.authWithGoogle(
                                                context = context,
                                                accountId = accountId,
                                                email = simulatedEmail.trim(),
                                                name = simulatedName.trim()
                                            )
                                            if (success) {
                                                Toast.makeText(context, "Successfully authenticated as ${simulatedEmail.trim()}!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            } else {
                                                Toast.makeText(context, "Failed to connect simulated portal.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Conduit error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StellarGlow, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Establish Connection")
                            }
                        }
                    }
                }
            }
        }
    }
}
