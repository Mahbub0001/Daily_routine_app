package com.example.data.repository

import com.example.data.local.HabitDao
import com.example.data.models.Habit
import com.example.data.models.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteCompletionsForHabit(habit.id)
        habitDao.deleteHabit(habit)
    }

    suspend fun deleteHabitById(id: Int) {
        habitDao.deleteCompletionsForHabit(id)
        habitDao.deleteHabitById(id)
    }

    suspend fun toggleHabitCompletion(habit: Habit, dateString: String): Boolean {
        val completions = habitDao.getCompletionsByHabitId(habit.id)
        val isCompleted = completions.any { it.dateString == dateString }

        if (isCompleted) {
            // Uncomplete today
            habitDao.deleteCompletion(habit.id, dateString)
            
            // Adjust streak
            val newStreak = (habit.streak - 1).coerceAtLeast(0)
            habitDao.updateHabit(habit.copy(
                streak = newStreak,
                lastCompleted = if (newStreak == 0) 0L else habit.lastCompleted // keep or clear
            ))
            return false
        } else {
            // Complete today
            val todayCompletion = HabitCompletion(
                habitId = habit.id,
                dateString = dateString,
                timestamp = System.currentTimeMillis()
            )
            habitDao.insertCompletion(todayCompletion)

            // Adjust streak
            val lastCompletedMillis = habit.lastCompleted
            val isYesterday = isYesterday(lastCompletedMillis)
            val isToday = isToday(lastCompletedMillis)

            val newStreak = when {
                isToday -> habit.streak // Alreay completed today on some level
                isYesterday -> habit.streak + 1 // Streak continues!
                else -> 1 // Streak reset/first time
            }

            habitDao.updateHabit(habit.copy(
                streak = newStreak,
                lastCompleted = System.currentTimeMillis()
            ))
            return true
        }
    }

    private fun isYesterday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = fmt.format(Date())
        val compStr = fmt.format(Date(timestamp))
        
        try {
            val todayInt = todayStr.toInt()
            val compInt = compStr.toInt()
            return (todayInt - compInt) == 1
        } catch (e: Exception) {
            // Fallback duration check
            val diff = System.currentTimeMillis() - timestamp
            return diff in 50000000..100000000 // roughly 14 to 28 hours
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date()) == fmt.format(Date(timestamp))
    }
}
