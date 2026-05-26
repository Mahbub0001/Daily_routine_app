package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.models.Habit
import com.example.data.models.HabitWithStatus
import com.example.api.FirebaseSyncHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatMessage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.SyncedDevice
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainHabitScreen(viewModel: RoutineViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val todayHabits by viewModel.todayHabits.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyAnalytics.collectAsStateWithLifecycle()
    val categoryDistribution by viewModel.categoryDistribution.collectAsStateWithLifecycle()
    
    val smartReminders by viewModel.smartReminders.collectAsStateWithLifecycle()
    val smartRemindersLoading by viewModel.smartRemindersLoading.collectAsStateWithLifecycle()
    
    val coachChatHistory by viewModel.coachChatHistory.collectAsStateWithLifecycle()
    val coachChatLoading by viewModel.coachChatLoading.collectAsStateWithLifecycle()
    
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val currentAppOpenStreak by viewModel.currentAppOpenStreak.collectAsStateWithLifecycle()
    val maxAppOpenStreak by viewModel.maxAppOpenStreak.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("today") } // "today", "stats", "coach", "profile"
    var showAddDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpaceBlack,
        floatingActionButton = {
            if (currentTab == "today") {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = StellarGlow,
                    contentColor = PureWhite,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_habit_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Habit", modifier = Modifier.size(28.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = if (isAppDarkThemeGlobal) Color(0xFF151518) else Color(0xFFF3EDF7),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .border(1.dp, BentoBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            ) {
                val tabs = listOf(
                    Triple("today", "Orbits", Icons.Default.Today),
                    Triple("stats", "Telemetry", Icons.Default.Analytics),
                    Triple("coach", "Midlu AI", Icons.Default.Forum),
                    Triple("profile", "Profile", Icons.Default.Person)
                )

                tabs.forEach { (tabId, label, icon) ->
                    val isActive = currentTab == tabId
                    NavigationBarItem(
                        selected = isActive,
                        onClick = { currentTab = tabId },
                        icon = {
                            Icon(
                                imageVector = icon, 
                                contentDescription = label,
                                tint = if (isActive) BentoTextPrimaryPurple else BentoTextSecondary
                            )
                        },
                        label = { 
                            Text(
                                text = label, 
                                fontSize = 11.sp, 
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) BentoTextPrimaryPurple else BentoTextSecondary
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoTextPrimaryPurple,
                            unselectedIconColor = BentoTextSecondary,
                            selectedTextColor = BentoTextPrimaryPurple,
                            unselectedTextColor = BentoTextSecondary,
                            indicatorColor = BentoCardPurple
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cosmic Sticky App Bar Header
            HeaderBlock()

            // Sub screen content container with switching transition animations
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState == "stats" || targetState == "coach" || targetState == "profile") width else -width } + fadeIn() with
                                slideOutHorizontally { width -> if (targetState == "today") width else -width } + fadeOut()
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "today" -> TodayOrbitView(
                            todayHabits = todayHabits,
                            smartReminders = smartReminders,
                            smartRemindersLoading = smartRemindersLoading,
                            onToggleHabit = { viewModel.toggleHabit(it) },
                            onEditHabit = { habitToEdit = it },
                            onDeleteHabit = { viewModel.deleteHabit(it.id) },
                            onRefreshReminders = { viewModel.refreshSmartReminders() }
                        )
                        "stats" -> TelemetryStatsView(
                            todayHabits = todayHabits,
                            weeklyStats = weeklyStats,
                            categoryDistribution = categoryDistribution,
                            currentAppOpenStreak = currentAppOpenStreak,
                            maxAppOpenStreak = maxAppOpenStreak
                        )
                        "coach" -> AetherGuideView(
                            history = coachChatHistory,
                            isLoading = coachChatLoading,
                            onSendMessage = { viewModel.askCoach(it) }
                        )
                        "profile" -> ProfileSectionView(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, cat, time ->
                viewModel.addHabit(title, desc, cat, time)
                showAddDialog = false
                Toast.makeText(context, "New orbit initialized!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (habitToEdit != null) {
        EditHabitDialog(
            habit = habitToEdit!!,
            onDismiss = { habitToEdit = null },
            onConfirm = { updatedHabit ->
                viewModel.editHabit(updatedHabit)
                habitToEdit = null
                Toast.makeText(context, "Orbit adjusted!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun HeaderBlock() {
    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val currentDateParts = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()).split(", ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MIDLU'S ROUTINE",
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = StellarGlow
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentDateParts.getOrNull(0) ?: "Today",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PureWhite
                )
                Text(
                    text = ", " + (currentDateParts.getOrNull(1) ?: ""),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = MutedSlate
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(BentoCardPurple)
                .border(1.dp, BentoBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BentoPrimaryPurple)
                )
                Text(
                    text = currentTime,
                    color = BentoTextPrimaryPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TodayOrbitView(
    todayHabits: List<HabitWithStatus>,
    smartReminders: String,
    smartRemindersLoading: Boolean,
    onToggleHabit: (Habit) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onRefreshReminders: () -> Unit
) {
    val totalCount = todayHabits.size
    val completedCount = todayHabits.count { it.isCompletedToday }
    val progressRatio = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ProgressRing"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Core Circular Dashboard Ring Card
        item {
            HabitsProgressRingCard(completedCount, totalCount, animatedProgress)
        }

        // Aether Smart suggestion
        item {
            AetherSmartSuggestions(
                content = smartReminders,
                isLoading = smartRemindersLoading,
                onRefresh = onRefreshReminders
            )
        }

        // Header and counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Orbit",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = "$completedCount/$totalCount Completed",
                    fontSize = 13.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (todayHabits.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "No orbits",
                            tint = MutedSlate.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "No routines active yet",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MutedSlate
                        )
                        Text(
                            text = "Click the [+] button below to build an orbit ritual.",
                            fontSize = 12.sp,
                            color = MutedSlate.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(todayHabits, key = { it.habit.id }) { item ->
                HabitOrbitRow(
                    item = item,
                    onToggle = { onToggleHabit(item.habit) },
                    onEdit = { onEditHabit(item.habit) },
                    onDelete = { onDeleteHabit(item.habit) }
                )
            }
        }
    }
}

@Composable
fun HabitsProgressRingCard(completed: Int, total: Int, progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_ring_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardPurple),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Orbit Velocity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BentoTextPrimaryPurple
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Complete current items to enter stable alignment.",
                    color = BentoTextPrimaryPurple.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Streak Logo", tint = BentoTextPrimaryPurple, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Perfect ritual synclink active",
                        color = BentoTextPrimaryPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = BentoBg,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // Progress Circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = BentoPrimaryPurple,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BentoTextPrimaryPurple
                    )
                    Text(
                        text = "$completed/$total",
                        color = BentoTextPrimaryPurple.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AetherSmartSuggestions(
    content: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
        border = BorderStroke(1.dp, BentoBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cyclone, 
                        contentDescription = "Midlu Spark", 
                        tint = BentoPrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MIDLU AI GUIDANCE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = BentoPrimaryPurple
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = BentoPrimaryPurple, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Insights", tint = BentoTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading && content.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BentoPrimaryPurple)
                }
            } else {
                Text(
                    text = content.ifBlank { "Initiating galactic recommendation engines..." },
                    color = BentoTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun HabitOrbitRow(
    item: HabitWithStatus,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedActions by remember { mutableStateOf(false) }

    val categoryColor = when (item.habit.category) {
        "Mind" -> SupernovaPink
        "Body" -> CosmicTeal
        "Work" -> StellarGlow
        else -> NebulaGold
    }

    val animatedCardBg by animateColorAsState(
        targetValue = if (item.isCompletedToday) DeepViolet.copy(alpha = 0.4f) else NebulaCard,
        animationSpec = tween(250),
        label = "cardBg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expandedActions = !expandedActions }
            .background(animatedCardBg)
            .border(
                width = 1.dp,
                color = if (item.isCompletedToday) CosmicTeal.copy(alpha = 0.2f) else TranslucentWhite.copy(alpha = 0.04f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Success checkbox selector circle with feedback bounce
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (item.isCompletedToday) CosmicTeal else SpaceBlack)
                    .border(
                        1.5.dp, 
                        if (item.isCompletedToday) CosmicTeal else MutedSlate.copy(alpha = 0.6f), 
                        CircleShape
                    )
                    .clickable { onToggle() }
                    .testTag("toggle_habit_${item.habit.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompletedToday) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = "Completed", 
                        tint = SpaceBlack, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.habit.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (item.isCompletedToday) MutedSlate else PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.habit.category.uppercase(),
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.habit.description,
                    color = MutedSlate,
                    fontSize = 12.sp,
                    maxLines = if (expandedActions) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Minimal Clock indicators & Streak
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.habit.targetTime,
                    fontSize = 12.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = "streak", tint = NebulaGold, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${item.habit.streak}d",
                        fontSize = 11.sp,
                        color = NebulaGold,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Expanded Action Drawer
        AnimatedVisibility(
            visible = expandedActions,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Divider(
                    color = TranslucentWhite.copy(alpha = 0.05f), 
                    modifier = Modifier.padding(vertical = 12.0.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Orbit Parameters: ",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = onEdit,
                        colors = ButtonDefaults.textButtonColors(contentColor = StellarGlow)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = SupernovaPink)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryStatsView(
    todayHabits: List<HabitWithStatus>,
    weeklyStats: List<com.example.ui.viewmodel.DayCompletionRate>,
    categoryDistribution: Map<String, Int>,
    currentAppOpenStreak: Int,
    maxAppOpenStreak: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Performance Analytics",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BentoTextPrimary
            )
        }

        // Stats Summary Dashboard
        item {
            val todayPercentage = if (todayHabits.isEmpty()) 0 else (todayHabits.count { it.isCompletedToday } * 100) / todayHabits.size
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak Card showing both current daily opening streak and overall max streak
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = "Daily open streak", tint = Color(0xFFE08A00), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Current Streak", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$currentAppOpenStreak days", color = BentoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Max Streak: $maxAppOpenStreak days", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Rates Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardBlue),
                    border = null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(imageVector = Icons.Default.QueryStats, contentDescription = "Consistency Rate", tint = BentoTextPrimaryBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Completed Today", color = BentoTextPrimaryBlue.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$todayPercentage%", color = BentoTextPrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Weekly Telemetry Parity Graph (CUSTOM CANVAS CHART)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Weekly Completion Rates",
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Quantity of synchronized completed rituals across nodes.",
                        color = BentoTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Draw the beautiful chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxCount = weeklyStats.map { it.completedCount }.maxOrNull()?.coerceAtLeast(1) ?: 1
                            val spacing = size.width / 6.5f
                            
                            // Draw grid lines
                            val gridCount = 3
                            for (grid in 0..gridCount) {
                                val gridY = size.height - (size.height / gridCount * grid)
                                drawLine(
                                    color = BentoBorder.copy(alpha = 0.3f),
                                    start = Offset(0f, gridY),
                                    end = Offset(size.width, gridY),
                                    strokeWidth = 2f
                                )
                            }

                            // Draw bars/points
                            weeklyStats.forEachIndexed { index, dayRate ->
                                val x = index * spacing + (spacing / 2f)
                                val ratio = dayRate.completedCount.toFloat() / maxCount.toFloat()
                                val y = size.height - (ratio * (size.height - 20f))

                                // Draw individual bar background
                                drawRoundRect(
                                    color = BentoBg,
                                    topLeft = Offset(x - 12f, 0f),
                                    size = Size(24f, size.height),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )

                                // Draw completed bar with elegant purple gradient brush
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(BentoPrimaryPurple, BentoCardPurple)),
                                    topLeft = Offset(x - 12f, y),
                                    size = Size(24f, size.height - y),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days label row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weeklyStats.forEach { dayRate ->
                            Box(
                                modifier = Modifier.width(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayRate.dayName,
                                        color = BentoTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${dayRate.completedCount}",
                                        color = if (dayRate.completedCount > 0) BentoPrimaryPurple else BentoTextSecondary.copy(alpha = 0.4f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Orbit Allocation Matrix",
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryDistribution.isEmpty()) {
                        Text("Register habits to visualize allocations.", color = BentoTextSecondary, fontSize = 12.sp)
                    } else {
                        val totalHabitsCount = categoryDistribution.values.sum().toFloat()
                        categoryDistribution.forEach { (cat, count) ->
                            val percent = (count.toFloat() / totalHabitsCount * 100).toInt()
                            val catColor = when (cat) {
                                "Mind" -> Color(0xFFC355F5)
                                "Body" -> BentoTextPrimaryBlue
                                "Work" -> BentoPrimaryPurple
                                else -> Color(0xFFE08A00)
                            }

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = cat, color = BentoTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "$percent% ($count)", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(BentoBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(count.toFloat() / totalHabitsCount)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AetherGuideView(
    history: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit
) {
    var rawInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val quickQuestions = listOf(
        "Design standard morning core routine",
        "Explain habits-stacking technique",
        "Help with screen-time winding habits",
        "Suggest body-recovery routines"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Midlu AI Guidance",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = PureWhite,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Advice Fast Queries Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(quickQuestions) { query ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NebulaCard)
                        .border(1.dp, TranslucentWhite.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { onSendMessage(query) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = query, color = CosmicTeal, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Conversational list
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
            border = BorderStroke(1.dp, BentoBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = true // Scroll to bottom when new messages join!
                ) {
                    // Reverse list for easy chat flow
                    val reversedHistory = history.reversed()
                    items(reversedHistory) { message ->
                        ChatBubbleRow(message = message)
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 16.dp, topStart = 16.dp, topEnd = 16.dp))
                                        .background(NebulaCard)
                                        .border(1.dp, StellarGlow.copy(alpha = 0.2f), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 16.dp, topStart = 16.dp, topEnd = 16.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = StellarGlow, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                                        Text("Decoding query structure...", color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // TextInput container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NebulaCard)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = it },
                        placeholder = { Text("Consult with Midlu AI...", color = MutedSlate, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coach_chat_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SpaceBlack,
                            unfocusedContainerColor = SpaceBlack,
                            focusedBorderColor = StellarGlow,
                            unfocusedBorderColor = TranslucentWhite.copy(alpha = 0.1f),
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )

                    IconButton(
                        onClick = {
                            if (rawInput.isNotBlank()) {
                                onSendMessage(rawInput)
                                rawInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StellarGlow)
                            .testTag("coach_send_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send advice", tint = PureWhite, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChatBubbleRow(message: ChatMessage) {
    val isAI = message.sender == "AI"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        val bubbleShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isAI) 4.dp else 20.dp,
            bottomEnd = if (isAI) 20.dp else 4.dp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(bubbleShape)
                .background(if (isAI) BentoCardPurple else BentoPrimaryPurple)
                .border(
                    width = 1.dp,
                    color = if (isAI) BentoBorder else Color.Transparent,
                    shape = bubbleShape
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isAI) Icons.Default.Cyclone else Icons.Default.Face, 
                        contentDescription = message.sender,
                        tint = if (isAI) BentoTextPrimaryPurple else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isAI) "Midlu AI" else "You",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isAI) BentoTextPrimaryPurple else Color.White.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.text,
                    color = if (isAI) BentoTextPrimary else Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ProfileSectionView(
    viewModel: RoutineViewModel
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("midlu_routine_prefs", android.content.Context.MODE_PRIVATE) }
    
    val userEmail by FirebaseSyncHelper.userEmail.collectAsState()
    val userName by FirebaseSyncHelper.userName.collectAsState()
    val firestoreStatus by FirebaseSyncHelper.firestoreStatus.collectAsState()
    
    // User Info state backed by SharedPreferences
    var profileName by remember { mutableStateOf(userName ?: "Astro Architect") }
    
    LaunchedEffect(userName) {
        if (userName != null) {
            profileName = userName!!
        }
    }
    
    var profileTitle by remember { mutableStateOf(prefs.getString("profile_title", "Orbit Architect") ?: "Orbit Architect") }
    var profileBio by remember { mutableStateOf(prefs.getString("profile_bio", "Aligning physical and temporal routines in spacetime.") ?: "Aligning physical and temporal routines in spacetime.") }
    
    // Theme option state (directly synced with the global state isAppDarkThemeGlobal)
    var isDarkTheme by remember { mutableStateOf(isAppDarkThemeGlobal) }

    // Coach Personality state backed by SharedPreferences
    var coachPersonality by remember { mutableStateOf(prefs.getString("coach_personality", "Midlu Guide") ?: "Midlu Guide") }
    
    // Dialog state for editing profile
    var showEditProfileDialog by remember { mutableStateOf(false) }
    
    // Stats
    val todayHabits by viewModel.todayHabits.collectAsStateWithLifecycle()
    val totalOrbits = todayHabits.size
    val completedOrbits = todayHabits.count { it.isCompletedToday }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Section Title
        item {
            Text(
                text = "Architect Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BentoTextPrimary
            )
        }

        // Beautiful Profile Card (Bento style)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar with an elegant gradient or letters
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        BentoPrimaryPurple,
                                        BeautifulRedAccent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profileName.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = profileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BentoTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = profileTitle,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = BentoPrimaryPurple,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = profileBio,
                        fontSize = 12.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoCardPurple.copy(alpha = 0.5f),
                            contentColor = BentoTextPrimaryPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Modify Details", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Stats summary bento row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Orbits Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL ORBITS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalOrbits",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = BentoTextPrimaryPurple
                        )
                    }
                }

                // Completed today Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                    border = BorderStroke(1.dp, BentoBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ALIGNED TODAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$completedOrbits/$totalOrbits",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (completedOrbits == totalOrbits && totalOrbits > 0) BeautifulRedAccent else BentoTextPrimaryBlue
                        )
                    }
                }
            }
        }

        // Theme and Preference Controls Section
        item {
            Text(
                text = "Preferences & Tuning",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BentoTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Theme Toggle Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Dark theme toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BentoCardBlue.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Theme Icon",
                                    tint = BentoTextPrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Space Dark Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = if (isDarkTheme) "Deep space theme active" else "Warm solar theme active",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }
                        
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isChecked ->
                                isDarkTheme = isChecked
                                isAppDarkThemeGlobal = isChecked
                                prefs.edit().putBoolean("app_theme_dark", isChecked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoPrimaryPurple,
                                checkedTrackColor = BentoCardPurple
                            ),
                            modifier = Modifier.testTag("theme_switch_toggle")
                        )
                    }

                    HorizontalDivider(color = TranslucentWhite.copy(alpha = 0.05f))

                    // Coach Personality Dropdown/Selector Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BentoCardPurple.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Coach Icon",
                                    tint = BentoTextPrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Midlu AI Guidance Voice",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BentoTextPrimary
                                )
                                Text(
                                    text = "Current: $coachPersonality Style",
                                    fontSize = 11.sp,
                                    color = BentoTextSecondary
                                )
                            }
                        }

                        // Options Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val personalites = listOf("Midlu Guide", "Encouraging", "Stoic Guide", "Composed")
                            personalites.forEach { style ->
                                val isSelected = coachPersonality == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BentoPrimaryPurple else BentoBg)
                                        .border(1.dp, if (isSelected) BentoPrimaryPurple else BentoBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            coachPersonality = style
                                            prefs.edit().putString("coach_personality", style).apply()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.split(" ").first(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else BentoTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Technical Information Section (Architect credentials)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Spacetime Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BentoTextPrimary
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "App Creator & Owner", fontSize = 11.sp, color = BentoTextSecondary)
                        Text(text = "Md Tahmid Hossain (Class 10)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StellarGlow)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Connected Account", fontSize = 11.sp, color = BentoTextSecondary)
                        Text(text = userEmail ?: "Local Anonymous", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Cloud Sync (Firestore)", fontSize = 11.sp, color = BentoTextSecondary)
                        Text(text = firestoreStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BeautifulBlueAccent)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "App Engine Version", fontSize = 11.sp, color = BentoTextSecondary)
                        Text(text = "v2.5.0-Firestore", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sub-system Protocol", fontSize = 11.sp, color = BentoTextSecondary)
                        Text(text = "AES-256 GCM Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoTextPrimary)
                    }

                    HorizontalDivider(color = TranslucentWhite.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

                    Button(
                        onClick = {
                            FirebaseSyncHelper.signOut(context)
                            Toast.makeText(context, "Secure session terminated. Safe travels!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("sign_out_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SupernovaPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Log Out Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Disconnect Portals (Sign Out)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = BentoCardWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                var nameInput by remember { mutableStateOf(profileName) }
                var titleInput by remember { mutableStateOf(profileTitle) }
                var bioInput by remember { mutableStateOf(profileBio) }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Modify Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BentoTextPrimary
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Architect Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Occupational Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Vocation Description / Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditProfileDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = BentoTextSecondary)
                        ) {
                            Text("Abort")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                profileName = nameInput
                                profileTitle = titleInput
                                profileBio = bioInput
                                prefs.edit()
                                    .putString("profile_name", nameInput)
                                    .putString("profile_title", titleInput)
                                    .putString("profile_bio", bioInput)
                                    .apply()
                                showEditProfileDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryPurple, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Commit")
                        }
                    }
                }
            }
        }
    }
}


// DIALOGS & OVERLAYS

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Work") } // Mind, Body, Work, Habits
    var hour by remember { mutableStateOf("08") }
    var min by remember { mutableStateOf("00") }

    val categories = listOf("Mind", "Body", "Work", "Routine")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCardWhite)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Orbit Ritual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BentoTextPrimary
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ritual Title (e.g., Hydrate)", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoPrimaryPurple,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimaryPurple,
                        unfocusedLabelColor = BentoTextSecondary,
                        cursorColor = BentoPrimaryPurple
                    ),
                    singleLine = true
                )

                // Desc Input
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Instructional Description", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoPrimaryPurple,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimaryPurple,
                        unfocusedLabelColor = BentoTextSecondary,
                        cursorColor = BentoPrimaryPurple
                    )
                )

                // Category selector chip row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Orbit Category", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            val catColor = when (cat) {
                                "Mind" -> Color(0xFFC355F5)
                                "Body" -> BentoTextPrimaryBlue
                                "Work" -> BentoPrimaryPurple
                                else -> Color(0xFFE08A00)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) catColor else BentoBg)
                                    .border(1.dp, if (isSelected) catColor else BentoBorder, RoundedCornerShape(12.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("category_chip_$cat")
                            ) {
                                Text(
                                    text = cat.uppercase(),
                                    color = if (isSelected) Color.White else BentoTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Hour", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hour,
                            onValueChange = { if (it.length <= 2) hour = it },
                            placeholder = { Text("08") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary,
                                focusedBorderColor = BentoPrimaryPurple,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryPurple,
                                unfocusedLabelColor = BentoTextSecondary,
                                cursorColor = BentoPrimaryPurple
                            ),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Minute", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = min,
                            onValueChange = { if (it.length <= 2) min = it },
                            placeholder = { Text("00") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary,
                                focusedBorderColor = BentoPrimaryPurple,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryPurple,
                                unfocusedLabelColor = BentoTextSecondary,
                                cursorColor = BentoPrimaryPurple
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confirmation actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = BentoTextSecondary)) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val formattedHour = hour.padStart(2, '0')
                            val formattedMin = min.padStart(2, '0')
                            onConfirm(title, desc, category, "$formattedHour:$formattedMin")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("confirm_add_habit_button")
                    ) {
                        Text("Initialize Orbit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (Habit) -> Unit
) {
    var title by remember { mutableStateOf(habit.title) }
    var desc by remember { mutableStateOf(habit.description) }
    var category by remember { mutableStateOf(habit.category) }
    val timeParts = habit.targetTime.split(":")
    var hour by remember { mutableStateOf(timeParts.getOrNull(0) ?: "08") }
    var min by remember { mutableStateOf(timeParts.getOrNull(1) ?: "00") }

    val categories = listOf("Mind", "Body", "Work", "Routine")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = BentoCardWhite)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Refine Orbit ritual",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BentoTextPrimary
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Ritual Title", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoPrimaryPurple,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimaryPurple,
                        unfocusedLabelColor = BentoTextSecondary,
                        cursorColor = BentoPrimaryPurple
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Instructional Description", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoPrimaryPurple,
                        unfocusedBorderColor = BentoBorder,
                        focusedLabelColor = BentoPrimaryPurple,
                        unfocusedLabelColor = BentoTextSecondary,
                        cursorColor = BentoPrimaryPurple
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Orbit Category", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            val catColor = when (cat) {
                                "Mind" -> Color(0xFFC355F5)
                                "Body" -> BentoTextPrimaryBlue
                                "Work" -> BentoPrimaryPurple
                                else -> Color(0xFFE08A00)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) catColor else BentoBg)
                                    .border(1.dp, if (isSelected) catColor else BentoBorder, RoundedCornerShape(12.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat.uppercase(),
                                    color = if (isSelected) Color.White else BentoTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Hour", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hour,
                            onValueChange = { if (it.length <= 2) hour = it },
                            placeholder = { Text("08") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary,
                                focusedBorderColor = BentoPrimaryPurple,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryPurple,
                                unfocusedLabelColor = BentoTextSecondary,
                                cursorColor = BentoPrimaryPurple
                            ),
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Target Minute", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = min,
                            onValueChange = { if (it.length <= 2) min = it },
                            placeholder = { Text("00") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary,
                                focusedBorderColor = BentoPrimaryPurple,
                                unfocusedBorderColor = BentoBorder,
                                focusedLabelColor = BentoPrimaryPurple,
                                unfocusedLabelColor = BentoTextSecondary,
                                cursorColor = BentoPrimaryPurple
                            ),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = BentoTextSecondary)) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val formattedHour = hour.padStart(2, '0')
                            val formattedMin = min.padStart(2, '0')
                            onConfirm(habit.copy(
                                title = title,
                                description = desc,
                                category = category,
                                targetTime = "$formattedHour:$formattedMin"
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimaryPurple, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply Adjustments", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
