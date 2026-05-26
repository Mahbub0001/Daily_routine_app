package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.MainHabitScreen
import com.example.ui.screens.AuthScreen
import com.example.api.FirebaseSyncHelper
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SupernovaPink
import com.example.ui.viewmodel.RoutineViewModel
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize FirebaseSyncHelper securely
        try {
            FirebaseSyncHelper.initialize(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Firebase initialization error: ${e.message}")
        }
        
        // Load persistent theme preference
        try {
            val prefs = getSharedPreferences("midlu_routine_prefs", Context.MODE_PRIVATE)
            com.example.ui.theme.isAppDarkThemeGlobal = prefs.getBoolean("app_theme_dark", false)
        } catch (e: Exception) {
            // Fallback gracefully
        }
        
        // Capture all uncaught exceptions to show Recovery Console upon next boot
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable)
                getSharedPreferences("midlu_routine_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("last_crash_trace", trace)
                    .commit()
            } catch (e: Exception) {
                // Ignore fallback writing failures
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var activeCrashLog by remember {
                    mutableStateOf(
                        getSharedPreferences("midlu_routine_prefs", Context.MODE_PRIVATE)
                            .getString("last_crash_trace", null)
                    )
                }

                val isUserAuthenticated by FirebaseSyncHelper.isUserAuthenticated.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (activeCrashLog != null) {
                        DiagnosticRecoveryConsole(
                            trace = activeCrashLog ?: "",
                            onClearDatabase = {
                                try {
                                    deleteDatabase("nova_routine_db")
                                } catch (e: Exception) {
                                    // Ignore database deletion errors
                                }
                                getSharedPreferences("midlu_routine_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .remove("last_crash_trace")
                                    .commit()
                                // Restart application cleanly
                                val intent = packageManager.getLaunchIntentForPackage(packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                exitProcess(0)
                            },
                            onDismiss = {
                                getSharedPreferences("midlu_routine_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .remove("last_crash_trace")
                                    .commit()
                                activeCrashLog = null
                            }
                        )
                    } else if (!isUserAuthenticated) {
                        AuthScreen(
                            onAuthSuccess = {
                                viewModel.refreshSmartReminders()
                            }
                        )
                    } else {
                        MainHabitScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRecoveryConsole(
    trace: String,
    onClearDatabase: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121214))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning Icon",
            tint = SupernovaPink,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "COSMIC EMERGENCY RECOVERY",
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Color.White,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "An unexpected disruption occurred in the spacetime alignment. Use the controls below to restore routine operations.",
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Scrollable traceback console
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = trace.ifBlank { "No Traceback context captured." },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFFF8B80),
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onClearDatabase,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SupernovaPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Quantum Reset",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E36)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Re-Entry",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}

