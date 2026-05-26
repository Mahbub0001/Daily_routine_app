package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.api.FirebaseSyncHelper
import com.example.data.local.AppDatabase
import com.example.data.models.Habit
import com.example.data.models.HabitCompletion
import com.example.data.models.HabitWithStatus
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HabitRepository(db.habitDao())

    val todayDateString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // All historic habits and completions from Room
    val habits: Flow<List<Habit>> = repository.allHabits
    val completions: Flow<List<HabitCompletion>> = repository.allCompletions

    // Reactive list pairing habits with completion state for today
    val todayHabits: StateFlow<List<HabitWithStatus>> = combine(habits, completions) { habitsList, completionsList ->
        val todayStr = todayDateString
        val completedIds = completionsList
            .filter { it.dateString == todayStr }
            .map { it.habitId }
            .toSet()

        habitsList.map { habit ->
            HabitWithStatus(
                habit = habit,
                isCompletedToday = completedIds.contains(habit.id)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Category Distribution for Stats
    val categoryDistribution: StateFlow<Map<String, Int>> = habits.map { list ->
        list.groupBy { it.category }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Weekly analytics of completion rates (Past 7 days)
    val weeklyAnalytics: StateFlow<List<DayCompletionRate>> = completions.map { completionList ->
        calculateWeeklyCompletions(completionList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI smart reminders state
    private val _smartReminders = MutableStateFlow("")
    val smartReminders: StateFlow<String> = _smartReminders.asStateFlow()

    private val _smartRemindersLoading = MutableStateFlow(false)
    val smartRemindersLoading: StateFlow<Boolean> = _smartRemindersLoading.asStateFlow()

    // AI Coach Chat
    private val _coachChatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                text = "✨ Salutations, developer of rituals! I am **Aether**, your intelligent habit mentor. I analyze your consistency patterns and suggest structural optimizations to level up your focus.",
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val coachChatHistory: StateFlow<List<ChatMessage>> = _coachChatHistory.asStateFlow()

    private val _coachChatLoading = MutableStateFlow(false)
    val coachChatLoading: StateFlow<Boolean> = _coachChatLoading.asStateFlow()

    // Sync state
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // App opening streak state
    private val _currentAppOpenStreak = MutableStateFlow(0)
    val currentAppOpenStreak: StateFlow<Int> = _currentAppOpenStreak.asStateFlow()

    private val _maxAppOpenStreak = MutableStateFlow(0)
    val maxAppOpenStreak: StateFlow<Int> = _maxAppOpenStreak.asStateFlow()

    init {
        // Track the daily app-opening streak
        trackAppOpenStreak()

        // Prepopulate with elegant habits on very first launch to enhance UX straight out of the box!
        viewModelScope.launch {
            try {
                habits.first().let { currentList ->
                    if (currentList.isEmpty()) {
                        prepopulateDefaultHabits()
                    }
                }
                refreshSmartReminders()
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error prepopulating default habits or loading reminders: ${e.message}", e)
            }
        }

        // Collect and auto-synchronize habits live to Firestore Database (real-time data sharing)
        viewModelScope.launch {
            habits.collect { habitsList ->
                try {
                    FirebaseSyncHelper.syncHabitsToCloud(habitsList)
                } catch (e: Exception) {
                    Log.e("RoutineViewModel", "Firestore sync exception: ${e.message}")
                }
            }
        }
    }

    private fun trackAppOpenStreak() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("midlu_routine_prefs", android.content.Context.MODE_PRIVATE)
            val todayStr = todayDateString
            val lastOpenDate = prefs.getString("last_open_date", null)
            var currentStreak = prefs.getInt("current_app_open_streak", 0)
            var maxStreak = prefs.getInt("max_app_open_streak", 0)

            if (lastOpenDate == null) {
                // Very first launch! Set streak to 1
                currentStreak = 1
                maxStreak = 1
                prefs.edit()
                    .putString("last_open_date", todayStr)
                    .putInt("current_app_open_streak", currentStreak)
                    .putInt("max_app_open_streak", maxStreak)
                    .apply()
            } else if (lastOpenDate != todayStr) {
                // Check if last open date was yesterday
                val isYesterday = checkIfYesterday(lastOpenDate)
                if (isYesterday) {
                    currentStreak += 1
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                } else {
                    // Missed a day! Break streak and reset to 1 (since they are opening the app now today!)
                    currentStreak = 1
                }
                prefs.edit()
                    .putString("last_open_date", todayStr)
                    .putInt("current_app_open_streak", currentStreak)
                    .putInt("max_app_open_streak", maxStreak)
                    .apply()
            }

            _currentAppOpenStreak.value = currentStreak
            _maxAppOpenStreak.value = maxStreak
        } catch (e: Exception) {
            Log.e("RoutineViewModel", "Error tracking app open streak: ${e.message}", e)
        }
    }

    private fun checkIfYesterday(dateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDate = sdf.parse(dateStr) ?: return false
            val todayDate = sdf.parse(todayDateString) ?: return false
            
            val diff = todayDate.time - lastDate.time
            val diffDays = diff / (24 * 60 * 60 * 1000)
            diffDays == 1L
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun prepopulateDefaultHabits() {
        val defaults = listOf(
            Habit(title = "Morning Meditation", description = "Deep breathing & mindfulness before checking screens", category = "Mind", targetTime = "07:30", streak = 0),
            Habit(title = "Deep Focus Study", description = "25-minute Pomodoro block on core technical skills", category = "Work", targetTime = "09:00", streak = 0),
            Habit(title = "Hydration Challenge", description = "Drink 500ml water to kickstart cellular metabolism", category = "Body", targetTime = "08:00", streak = 0),
            Habit(title = "Cosmic Workspace Cleanse", description = "De-clutter desk to foster sharp focus", category = "Routine", targetTime = "18:00", streak = 0)
        )
        for (habit in defaults) {
            repository.insertHabit(habit)
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                repository.toggleHabitCompletion(habit, todayDateString)
                // Stash and update smart notifications silently as they complete
                triggerAsyncReminderUpdate()
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error toggling habit: ${e.message}", e)
            }
        }
    }

    fun addHabit(title: String, description: String, category: String, time: String) {
        viewModelScope.launch {
            try {
                val h = Habit(
                    title = title.ifBlank { "Untitled Habit" },
                    description = description.ifBlank { "Unscheduled daily ritual" },
                    category = category,
                    targetTime = time
                )
                repository.insertHabit(h)
                triggerAsyncReminderUpdate()
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error adding habit: ${e.message}", e)
            }
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteHabitById(habitId)
                triggerAsyncReminderUpdate()
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error deleting habit: ${e.message}", e)
            }
        }
    }

    fun editHabit(habit: Habit) {
        viewModelScope.launch {
            try {
                repository.updateHabit(habit)
                triggerAsyncReminderUpdate()
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error editing habit: ${e.message}", e)
            }
        }
    }

    private fun triggerAsyncReminderUpdate() {
        viewModelScope.launch {
            try {
                val activeHabits = todayHabits.value.map { it.habit }
                val text = GeminiService.getIntelligentReminders(activeHabits)
                _smartReminders.value = text
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error in triggerAsyncReminderUpdate: ${e.message}", e)
            }
        }
    }

    fun refreshSmartReminders() {
        if (_smartRemindersLoading.value) return
        viewModelScope.launch {
            try {
                _smartRemindersLoading.value = true
                val activeHabits = todayHabits.value.map { it.habit }
                val text = GeminiService.getIntelligentReminders(activeHabits)
                _smartReminders.value = text
                _smartRemindersLoading.value = false
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error in refreshSmartReminders: ${e.message}", e)
                _smartRemindersLoading.value = false
            }
        }
    }

    fun askCoach(query: String) {
        if (query.isBlank() || _coachChatLoading.value) return
        val userMsg = ChatMessage(sender = "User", text = query, timestamp = System.currentTimeMillis())
        _coachChatHistory.value = _coachChatHistory.value + userMsg

        _coachChatLoading.value = true
        viewModelScope.launch {
            try {
                val activeHabits = todayHabits.value.map { it.habit }
                val response = GeminiService.askHabitCoach(query, activeHabits)
                val aiMsg = ChatMessage(sender = "AI", text = response, timestamp = System.currentTimeMillis())
                _coachChatHistory.value = _coachChatHistory.value + aiMsg
                _coachChatLoading.value = false
            } catch (e: Exception) {
                Log.e("RoutineViewModel", "Error asking coach: ${e.message}", e)
                val errMsg = ChatMessage(sender = "AI", text = "I failed to secure alignment with the quantum intelligence matrix. Please re-try sending your query.", timestamp = System.currentTimeMillis())
                _coachChatHistory.value = _coachChatHistory.value + errMsg
                _coachChatLoading.value = false
            }
        }
    }

    // Interactive Sync Simulator representing device syncing with simulated live status logs!
    fun triggerSync() {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                status = "Initiating cloud handshakes...",
                syncLogs = listOf("Sync initiated at UTC ${getCurrentTimeUtc()}", "Authenticating local node...") + _syncState.value.syncLogs
            )
            kotlinx.coroutines.delay(1200)

            _syncState.value = _syncState.value.copy(
                status = "Parsing delta transactions...",
                syncLogs = listOf("Node authentication successful", "Comparing database schemas...", "Syncing 4 table entities...") + _syncState.value.syncLogs
            )
            kotlinx.coroutines.delay(1000)

            _syncState.value = _syncState.value.copy(
                status = "Verifying telemetry parity...",
                syncLogs = listOf("Uploading completed habit milestones", "Compressing database logs [6.4 KB]...") + _syncState.value.syncLogs
            )
            kotlinx.coroutines.delay(1000)

            val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val lastSyncedTime = formatter.format(Date())

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSynced = lastSyncedTime,
                status = "Fully Synced",
                devices = listOf(
                    SyncedDevice("Galaxy Fold 6 (This Phone)", "Active Now", true),
                    SyncedDevice("iPad Pro (Tablet)", "Synced $lastSyncedTime", false),
                    SyncedDevice("Work MacBook (Desktop)", "Synced $lastSyncedTime", false),
                    SyncedDevice("Pixel Watch 2 (Wearable)", "Synced $lastSyncedTime", false)
                ),
                syncLogs = listOf("Sync successfully resolved in 3.2s.", "Sent 2 milestones, pulled 0 updates.", "Status verified: ALL DEVICES CONNECTED.") + _syncState.value.syncLogs
            )
        }
    }

    private fun getCurrentTimeUtc(): String {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(Date())
    }

    private fun calculateWeeklyCompletions(completions: List<HabitCompletion>): List<DayCompletionRate> {
        val calendar = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val result = mutableListOf<DayCompletionRate>()

        // Look back at the past 7 days (including today)
        for (i in 6 downTo 0) {
            val loopCal = calendar.clone() as Calendar
            loopCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = fmt.format(loopCal.time)

            // Calculate formatted day title, e.g. "Mon"
            val dayName = SimpleDateFormat("E", Locale.getDefault()).format(loopCal.time)
            
            // Completion rate for this day
            val countForDay = completions.filter { it.dateString == dateStr }.size
            result.add(DayCompletionRate(dateString = dateStr, dayName = dayName, completedCount = countForDay))
        }
        return result
    }
}

// Data Classes supporting statistics and synced logs
data class DayCompletionRate(
    val dateString: String,
    val dayName: String,
    val completedCount: Int
)

data class ChatMessage(
    val sender: String, // "User" or "AI"
    val text: String,
    val timestamp: Long
)

data class SyncedDevice(
    val name: String,
    val status: String,
    val isPrimary: Boolean
)

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSynced: String = "13:30",
    val status: String = "Online & Secured",
    val devices: List<SyncedDevice> = listOf(
        SyncedDevice("Galaxy Fold 6 (This Phone)", "Active Now", true),
        SyncedDevice("iPad Pro (Tablet)", "Synced 10 minutes ago", false),
        SyncedDevice("Work MacBook (Desktop)", "Synced 1 hour ago", false)
    ),
    val syncLogs: List<String> = listOf("Node authentication resolved.", "Local replica parity verified.")
)
