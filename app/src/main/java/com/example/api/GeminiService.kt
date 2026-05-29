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

        val systemInstruction = "You are 'Midlu AI', the cosmic daily routine consultant, a natural, supportive chatbot. Speak in an elegant, clear, and highly conversational frequency ('Midlu AI Guidance' tone). Be exceptionally brief and concise. Do NOT start your answers with repeating or paraphrasing the user's prompt (e.g., do NOT say 'Regarding your question...', 'You asked...', or 'I have analyzed your request...'). Instead, directly address their query with wise, actionable routine coaching. If the user asks who created, made, founded, or discovered you, proudly answer that you were created by Md Tahmid Hossain, an extraordinarily talented developer currently in class 10."

        val prompt = """
            User Habits:
            $habitsListStr
            
            Question: $query
            
            Write a natural, direct, and conversational reply to the question using the Midlu AI Guidance voice. Do not include templates, introductions, or mention their specific request. Be clear and extremely helpful.
        """.trimIndent()

        val response = makeApiCall(prompt, systemInstruction)
        if (response == "API_KEY_MISSING" || response.startsWith("Error")) {
            // Check for creator queries to maintain 100% compliance even in fallback mode
            val isCreatorReq = q.contains("create") || q.contains("made you") || q.contains("who made") ||
                    q.contains("founder") || q.contains("develop") || q.contains("build you") ||
                    q.contains("built you") || q.contains("programmed you") || q.contains("designed you") ||
                    q.contains("discover") || q.contains("find you") || q.contains("found you") ||
                    q.contains("maker") || q.contains("author") || q.contains("owner")

            if (isCreatorReq) {
                return """
                    ✨ **Midlu AI Protocol Alert** ✨
                    
                    I was designed, founded, and discovered by **Md Tahmid Hossain**, who is the owner of this routine application. He is an exceptionally talented creator who reads in **class 10**. 
                    
                    My core architecture and alignment are guided by his vision to bring structured orbital focusing and daily rituals to life.
                """.trimIndent()
            }

            return generateDynamicFallbackResponse(query, habits)
        }
        return response
    }

    /**
     * Generates a fully customized, topic-matched fallback response when Gemini APIs are unavailable.
     * Keeps answers exceptionally diverse, context-aware, helpful, and concise.
     */
    private fun generateDynamicFallbackResponse(query: String, habits: List<Habit>): String {
        val q = query.lowercase().trim()
        val favHabit = habits.firstOrNull()?.title ?: "Atomic Focus"
        val category = habits.firstOrNull()?.category ?: "Focus"

        // Handle Bengali/Bangla language greetings & check-ins
        if (q.contains("bangla") || q.contains("bengali") || q.contains("bujho") || q.contains("bujhbo") || q.contains("বাংলা")) {
            return """
                ✨ **Midlu AI Resonance**
                
                Yes, I fully comprehend Bengali. As your intelligent routine mentor, I am ready to guide you in any language.
                
                Let's structure your daily rituals and elevate your consistency. What are we building today?
            """.trimIndent()
        }

        // Handle basic greetings
        if (q == "hi" || q == "hello" || q == "hey" || q == "greetings" || q == "assalamualaikum" || q == "salam") {
            return """
                ✨ **Salutations, builder of consistency!**
                
                I am **Midlu AI**, your intelligent habit mentor. Let us refine your daily orbits and unlock peak focus. 
                
                Tell me what ritual you want to design, optimize, or scale today.
            """.trimIndent()
        }

        if (q.contains("how are") || q.contains("how 's it") || q.contains("hows it")) {
            return """
                🌌 **Midlu AI Alignment**
                
                My systems are operating at peak cosmic frequency, fully aligned to support your routine journey. 
                
                Let's channel this momentum into refining your habits or completing today's open completions.
            """.trimIndent()
        }

        // 1. Sleep, Morning, Night, Circadian
        if (q.contains("morning") || q.contains("wake") || q.contains("early") || q.contains("alarm") || q.contains("sun")) {
            return """
                🌅 **Midlu AI Morning Ritual Blueprint**
                
                Waking up with clear alignment is the anchor of peak daily execution:
                
                • **10-Sec Launchrule**: The moment your alarm rings, sit up instantly and plant both feet firmly on the ground. Refuse to let your mind negotiate with morning inertia.
                • **Morning Habit Stacking**: Anchor your day by placing your **$favHabit** priority directly after your first morning beverage.
                • **Light Capture**: Expose your eyes to direct natural light or bright light within 15 minutes of waking to optimize your circadian cycle.
                
                A legendary morning is built on reducing choices, not increasing them. Try this tomorrow!
            """.trimIndent()
        }
        
        if (q.contains("sleep") || q.contains("night") || q.contains("bed") || q.contains("insomnia") || q.contains("evening") || q.contains("sleepy")) {
            return """
                🌙 **Midlu AI Evening & Rest Optimization**
                
                Deep, restorative sleep is the fuel of atomic productivity and metabolic health:
                
                • **The 3-2-1 Cutoff Principle**: Stop eating heavy foods 3 hours before bed; stop dynamic workspace activities 2 hours before; cut off high-stimulus digital screens 1 hour before.
                • **Digital Sinking**: Place your cellular devices outside your sleeping chambers. Replace late-night scrolling with an analog reading habit or quiet meditation.
                • **Brain Siphoning**: Write down your top three tasks for tomorrow on paper right before bed to relieve subconscious cognitive overhead.
                
                Secure your sleep tonight so you can execute your rituals with razor-sharp precision tomorrow.
            """.trimIndent()
        }

        // 2. Study, Work, Productivity, Focus, Concentration, Learning
        if (q.contains("study") || q.contains("learn") || q.contains("work") || q.contains("productiv") || q.contains("focus") || q.contains("concentrat") || q.contains("career") || q.contains("code") || q.contains("task") || q.contains("exam")) {
            return """
                ⚡ **Midlu AI Deep-Focus Strategy**
                
                Deep work is an elite superpower in our highly distracted economy. Here is how to lock in concentration:
                
                • **The 25-5 Pomodoro Gate**: Set a countdown timer for exactly 25 minutes. Dedicate 100% of your cognitive energy to a single task (like your **$favHabit**). Zero tab-switching allowed.
                • **Environmental Isolation**: Clear your physical desk of all items except what you need for this exact task. Every visual clutter is a silent tax on attention.
                • **Attention Offloading**: When distracting thoughts emerge, quickly write them down in a "scratch pad" to review later, maintaining your current focus flow.
                
                Clear the field, start a countdown, and commit to 10 minutes of pure action.
            """.trimIndent()
        }

        // 3. Procrastination, Lazy, Motivation, Discipline
        if (q.contains("procrastin") || q.contains("lazy") || q.contains("motivat") || q.contains("delay") || q.contains("disciplin") || q.contains("boring") || q.contains("bored")) {
            return """
                🪐 **Midlu AI Momentum Blueprint**
                
                Procrastination is not a flaw of willpower; it is an emotional defense mechanism against initial friction:
                
                • **The 2-Minute Micro-Agreement**: Commit to doing the task (e.g., spending time on your **$favHabit**) for just 2 minutes. The physical friction of starting is 90% of the battle.
                • **Systems over Motivation**: Stop waiting for the "feeling" of high motivation. Treat consistency as a professional contract. Clear systems always outperform erratic inspiration.
                • **Friction Scaling**: Make starting simple. Lay out your work materials, open the software, or step into your active gear *the session before*.
                
                Action breeds motivation, not the other way around. Focus on taking one microscopic step right now.
            """.trimIndent()
        }

        // 4. Gym, Workout, Health, Exercise, Fitness, Diet, Water
        if (q.contains("gym") || q.contains("workout") || q.contains("exercise") || q.contains("run") || q.contains("walk") || q.contains("health") || q.contains("water") || q.contains("diet") || q.contains("fit") || q.contains("cardio") || q.contains("stretch")) {
            return """
                🏋️ **Midlu AI Physical Vitality Optimization**
                
                Your physical vessel is the reactor that powers all mental concentration and productivity:
                
                • **The Elastic Gym Concept**: If you don't have time for a full 60-minute session, perform 10 bodyweight movements or a swift 5-minute walk. Never break the chain of daily action.
                • **Hydration Anchoring**: Place an elegant container of water visually centered in your primary workspace. Drink 500ml of water immediately after waking.
                • **Friction Manipulation**: Pack your gear or put out your active clothing the night before. This eliminates morning decision fatigue entirely.
                
                Consistency at 50% capacity is infinitely superior to occasional perfection at 100%. Move your body today!
            """.trimIndent()
        }

        // 5. Bad habits, Quit, Stop, Screen time, Screens, Phone, Addiction
        if (q.contains("bad") || q.contains("quit") || q.contains("break") || q.contains("screen") || q.contains("phone") || q.contains("addic") || q.contains("scroll") || q.contains("stop") || q.contains("limit") || q.contains("time") || q.contains("distract")) {
            return """
                🛡️ **Midlu AI Habit-Breaking Protocol**
                
                Breaking a habit requires rewiring the loop of Trigger -> Craving -> Response -> Reward:
                
                • **Friction Amplification (The 20-Second Rule)**: Make the bad habit require at least 20 seconds of physical effort to begin. Put social media apps in deep folders, log out, or lock your phone away.
                • **Trigger Mapping**: Identify what initiates the bad behavior. Is it boredom, stress, or fatigue? Intercept the cue and replace the response with a positive alternative.
                • **The Identity Pivot**: Stop saying "I'm trying to quit scrolling." Say "I am a high-focus individual." Shift your self-image to align with your highest ambitions.
                
                Every time you resist a craving, you actively weaken the neurochemical pathways of that habit. Stand firm today!
            """.trimIndent()
        }

        // 6. Consistency, Streak, Routine, Atomic, Habit Stacking
        if (q.contains("streak") || q.contains("consist") || q.contains("plan") || q.contains("schedule") || q.contains("calendar") || q.contains("routine") || q.contains("stack") || q.contains("atomic")) {
            return """
                🎯 **Midlu AI Habit Architecture Masterclass**
                
                Designing a flawless consistency loop is an art. Utilize these principles to construct bulletproof schedules:
                
                • **Habit Stacking Principle**: The absolute best way to insert a new habit is to anchor it directly onto an existing, automatic habit (e.g., "After I pour my morning coffee, I will immediately practice 5 minutes of focused breathing").
                • **Never Miss Twice Rule**: Missing one day is an accident. Missing two days is the initiation of a new, negative habit. Secure your streak at all costs today!
                • **Low-Floor Launchpads**: Reduce your entry barriers. Make sure the initial effort of your **$favHabit** is so light that it's impossible to fail.
                
                You do not rise to the level of your goals; you fall to the level of your systems. Keep building those systems!
            """.trimIndent()
        }

        // 7. Core Concept Explanation / Definition
        if (q.contains("what is") || q.contains("explain") || q.contains("meaning") || q.contains("concept") || q.contains("define") || q.contains("how to")) {
            return """
                📓 **Midlu AI Behavioral Concept Explainer**
                
                Let's dissect the science of behavioral design:
                
                • **The Dopamine Reward System**: New habits are reinforced when the brain experiences a dopamine pocket. Celebrate immediately after completing your **$favHabit** to lock in the neural reward.
                • **Atomic Compounds**: Just as small compound interest builds fortunes, microscopic improvements in daily consistency accumulate massive personal transformation over time.
                • **Trigger-Response Anchors**: Place visual cues in your path. If you want to study regularly, leave your book open on the kitchen island.
                
                To successfully build routines, map out a simple 3-step sequence: trigger, action, and internal celebration.
            """.trimIndent()
        }

        // 8. General fall-back (diverse & randomized based on query)
        val responsePool = listOf(
            Pair(
                "Cosmic Consistency Protocol",
                listOf(
                    "**The Goldilocks Strategy**: Keep challenges balanced. Your rituals shouldn't be too easy to become boring, nor too hard to feel impossible.",
                    "**Environmental Clarity**: Make your desired habits visual, obvious, and attractive. Remove all friction between you and **$favHabit**.",
                    "**The Immediate Reward Loop**: Congratulate yourself silently right after finishing. This solidifies neural retention."
                )
            ),
            Pair(
                "Atomic Orbital Guidance",
                listOf(
                    "**Behavioral Stacking**: Anchor your desired action directly behind one of your current daily rituals (your **$favHabit** orbit is an excellent candidate).",
                    "**Friction Inversion**: Make the good path easy and the bad path insanely hard. Clean your desk before going to sleep.",
                    "**Microscopic Momentum**: Do not try to master everything instantly. Improve your daily consistency by just 1% each morning."
                )
            ),
            Pair(
                "Optimal Focus Blueprint",
                listOf(
                    "**Attention Preservation**: Block all notification popups and close unneeded tabs. Protect your raw cognitive focus like gold.",
                    "**Habit Anchor Points**: Specify exactly *when* and *where* you will execute your routines. Ambiguity is the quiet killer of habits.",
                    "**Identity-Led Architecture**: Shift your internal dialogue. Focus on the *type of person* you want to become rather than just the specific goal."
                )
            )
        )

        // Select based on length / hash of query to establish high diversity
        val selectionIndex = (q.length + q.hashCode()) % responsePool.size
        val selected = responsePool[Math.abs(selectionIndex)]
        val title = selected.first
        val bulletPoints = selected.second

        return """
            🌟 **Midlu AI $title**
            
            Based on active routines like **$favHabit** in the **$category** system, here are your tailored guidelines to optimize high-performance consistency:
            
            • ${bulletPoints[0]}
            • ${bulletPoints[1]}
            • ${bulletPoints[2]}
            
            *Let's maintain extreme discipline and secure your consistency loop.*
        """.trimIndent()
    }
}
