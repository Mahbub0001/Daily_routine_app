package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String, // "Mind", "Body", "Work", "Routine"
    val targetTime: String = "08:00", // Format e.g. "08:00"
    val createdAt: Long = System.currentTimeMillis(),
    val streak: Int = 0,
    val lastCompleted: Long = 0L // Timestamp of last completion
)

@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String, // Format YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)


data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "User" or "AI"
    val text: String,
    val timestamp: Long
)

