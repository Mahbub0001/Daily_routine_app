package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.models.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

        // Use valid models list according to SKILL.md. Add gemini-3.1-flash-lite-preview as a high-limit friendly fallback.
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")
        var lastErrorMsg = ""

        for (model in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            
            var attempts = 0
            val maxAttempts = 3
            var delayMs = 1500L

            while (attempts < maxAttempts) {
                attempts++
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
                        if (response.isSuccessful) {
                            if (!bodyString.isNullOrEmpty()) {
                                val jsonResponse = JSONObject(bodyString)
                                val candidates = jsonResponse.optJSONArray("candidates")
                                val firstCandidate = candidates?.optJSONObject(0)
                                val contentObj = firstCandidate?.optJSONObject("content")
                                val parts = contentObj?.optJSONArray("parts")
                                val firstPart = parts?.optJSONObject(0)
                                val responseText = firstPart?.optString("text")
                                if (responseText != null) {
                                    Log.i(TAG, "Successfully generated AI response utilizing model: $model on attempt $attempts")
                                    return@withContext responseText
                                }
                            }
                        } else {
                            val code = response.code
                            Log.w(TAG, "API call for model $model failed (Attempt $attempts/$maxAttempts) with code $code: $bodyString")
                            lastErrorMsg = "HTTP $code - ${response.message}"
                            
                            if (code == 429) {
                                if (attempts < maxAttempts) {
                                    Log.i(TAG, "Rate limited (429). Retrying in ${delayMs}ms...")
                                    delay(delayMs)
                                    delayMs *= 2
                                    continue
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during call for model $model (Attempt $attempts/$maxAttempts): ${e.message}", e)
                    lastErrorMsg = e.localizedMessage ?: "Connection failure"
                }
                
                break
            }
        }

        "Error calling AI coach. Detail: $lastErrorMsg"
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
        if (response == "API_KEY_MISSING" || response.startsWith("Error")) {
            val habitTitle = habits.firstOrNull()?.title ?: "your major habit"
            val category = habits.firstOrNull()?.category ?: "Focus"
            return """
                🚀 **Intelligent Synergy Alert**: 
                Optimize your **$habitTitle** ($category orbit) by pairing it with an immediate physical anchor (like deep ambient breathing or hydration). Stacking rituals reduces mental friction and secures atomic consistency.
                
                🌌 **Wisdom Byte**: 
                "A perfect daily loop is not built on sheer willpower, but on beautiful environmental design." Place a key physical trigger for **$habitTitle** visible in your focus room.
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
        if (response == "API_KEY_MISSING" || response.startsWith("Error")) {
            // Check for creator queries to maintain 100% compliance even in fallback mode
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

            val favHabit = habits.firstOrNull()?.title ?: "Atomic Focus"
            return """
                🌌 **Greetings from Midlu AI**
                
                Here is some custom wisdom on *"$query"*:
                
                • **Atomic Momentum**: When implementing *"$query"*, start with a step so simple it takes under 2 minutes. For instance, optimize your **$favHabit** ritual first.
                • **Environmental Design**: Make the positive route the path of least resistance by preparing visual and physical cues in advance.
                • **Habit Stacking**: Inject your desired new action directly behind an ultra-reliable habit you already perform daily.
            """.trimIndent()
        }
        return response
    }
}
