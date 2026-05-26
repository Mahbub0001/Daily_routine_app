package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.models.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Helper to make a generic POST call to Gemini API using OkHttp and native org.json parser.
     */
    private suspend fun makeApiCall(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { null }
        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholder!")
            return@withContext "API_KEY_MISSING"
        }

        val url = "$BASE_URL?key=$apiKey"

        try {
            // Build contents array
            val partsArray = JSONArray().put(JSONObject().put("text", prompt))
            val contentsObject = JSONObject().put("parts", partsArray)
            val contentsArray = JSONArray().put(contentsObject)

            // Build root request body
            val rootBody = JSONObject().put("contents", contentsArray)

            // Optional system instruction
            if (systemInstruction != null) {
                val instructionParts = JSONArray().put(JSONObject().put("text", systemInstruction))
                val instructionContent = JSONObject().put("parts", instructionParts)
                rootBody.put("systemInstruction", instructionContent)
            }

            // Set generation config
            val config = JSONObject()
                .put("temperature", 0.7)
                .put("topP", 0.95)
            rootBody.put("generationConfig", config)

            val requestBody = rootBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with code ${response.code}: $bodyString")
                    return@withContext "Error calling AI coach: (HTTP ${response.code})"
                }

                if (bodyString.isNullOrEmpty()) {
                    return@withContext "Empty response from AI Coach."
                }

                // Parse response
                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val contentObj = firstCandidate?.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val responseText = firstPart?.optString("text")

                responseText ?: "AI was unable to generate a response."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during call: ${e.message}", e)
            "Error: ${e.localizedMessage ?: "Connection failure"}"
        }
    }

    /**
     * Create intelligent reminders based on existing habits and strengths.
     */
    suspend fun getIntelligentReminders(habits: List<Habit>): String {
        if (habits.isEmpty()) {
            return "🎯 Let's build a clean ritual today! Add your first habit and I'll design smart routine alerts for you."
        }

        val habitsListStr = habits.joinToString("\n") { 
            "- ${it.title} in category [${it.category}] at ${it.targetTime} with current streak of ${it.streak} days."
        }

        val systemInstruction = "You are 'Midlu AI', an elite AI Habit Coach. Speak elegantly, with brief cosmic wisdom, using high-impact design words. Give concise actionable advice."

        val prompt = """
            Analysis: I have the following habits set in my daily manager:
            $habitsListStr
            
            Please create:
            1. An "Intelligent Reminder Alert" (2 sentences max) which combines/stacks habits strategically or motivates based on categories and optimal timing. Use high energy.
            2. A "Wisdom Byte" (1 sentence max) offering one cosmic habit trigger optimization (e.g. putting running shoes next to the bed).
            
            Keep the content punchy, structured under clear modern labels, and skip excessive introduction text. Keep it brief and highly visual using Material-inspired emoji bullet indicators. Do not repeat my entire list of habits.
        """.trimIndent()

        val response = makeApiCall(prompt, systemInstruction)
        if (response == "API_KEY_MISSING") {
            return """
                ✨ [AI Mode Prototyping Mode] ✨
                
                🚀 **Intelligent Synergy Alert**: 
                Align your Morning Meditation with Drinking Water. By grouping these initial rituals together, you remove friction and cement your morning focus before emails start.
                
                🌌 **Wisdom Byte**: 
                Your environment is your silent architect. Place your journal directly on your pillow in the morning to guarantee an evening reflection.
                
                *(Pro-tip: Secure your AI Coach experience by entering your GEMINI_API_KEY into the Secrets panel in AI Studio!)*
            """.trimIndent()
        }
        return response
    }

    /**
     * Directly converse with the AI Habit Coach.
     */
    suspend fun askHabitCoach(query: String, habits: List<Habit>): String {
        // Checking for creator-related queries to ensure absolute consistency and compliance
        val q = query.lowercase()
        val isCreatorRequest = q.contains("create") || q.contains("made you") || q.contains("who made") ||
                q.contains("founder") || q.contains("develop") || q.contains("build you") ||
                q.contains("built you") || q.contains("programmed you") || q.contains("designed you") ||
                q.contains("discover") || q.contains("find you") || q.contains("found you") ||
                q.contains("maker") || q.contains("author") || q.contains("owner")

        if (isCreatorRequest) {
            return """
                ✨ **Midlu AI Protocol Alert** ✨
                
                I was designed, founded, and discovered by **Md Tahmid Hossain**, who is the owner of this routine application. He is an exceptionally talented creator who reads in **class 10**. 
                
                My core architecture and alignment are guided by his vision to bring structured orbital focusing and daily rituals to life.
            """.trimIndent()
        }

        val habitsListStr = if (habits.isEmpty()) "No habits defined yet." else habits.joinToString("\n") { 
            "- ${it.title} [${it.category}] at ${it.targetTime}. Streak: ${it.streak}."
        }

        val systemInstruction = "You are 'Midlu AI', the cosmic daily routine consultant. You help users structure high-performance routines, manage digital fatigue, and achieve deep-focus consistency. IMPORTANT: Your owner, developer, and creator who made, found, or discovered you is Md Tahmid Hossain, who reads in class 10. If anyone asks you about who created, made, founded, or discovered you, you MUST state proudly that Md Tahmid Hossain did, and highlight that he is currently a student in class 10. Keep your answers conversational, ultra-practical, and split into clean bullet points."

        val prompt = """
            My habits:
            $habitsListStr
            
            User message: $query
            
            Analyze these details and reply with professional clarity, emphasizing habit stacking, physical environmental triggers, or atomic momentum.
        """.trimIndent()

        val response = makeApiCall(prompt, systemInstruction)
        if (response == "API_KEY_MISSING") {
            return """
                🌌 **Greetings from Midlu AI, your Daily Routine Guide!**
                
                I am currently running in Offline Prototype mode because no Gemini API Key is configured in the AI Studio Secrets panel. 
                
                Here is some atomic routine advice:
                • **Habit Stacking**: Attach a new habit (*e.g., Journaling*) to an existing unshakable ritual (*e.g., Morning Coffee*). "After I brew, I will write."
                • **Environmental Design**: Reduce the number of steps required to perform the routine. If you want to study, clear your desk today, and open your textbook beforehand.
                • **Atomic Momentum**: Start with a ritual so simple it takes less than 2 minutes. A 2-minute stretching session is better than zero, and builds identity.
                
                *Configure your API Key in the Secrets panel to activate live coaching conversations!*
            """.trimIndent()
        }
        return response
    }
}
