package com.example.leetcodegate.llm

import com.example.leetcodegate.data.LlmConfig
import com.google.gson.Gson
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.RequestBody.Companion.toRequestBody

data class LlmResponseSchema(
    val pass: Boolean,
    val detected_problem_number: String?,
    val reason: String,
    val interview_feedback: String
)

class LlmValidator(private val client: OkHttpClient, private val gson: Gson) {

    suspend fun validateExplanation(
        config: LlmConfig,
        problemId: String,
        explanation: String,
        ocrText: String
    ): LlmResponseSchema = suspendCancellableCoroutine { continuation ->

        val systemPrompt = """
            You are an expert technical interviewer at a top tech company.
            The user is submitting their explanation for how they solved a LeetCode problem.
            
            Target Problem ID: $problemId
            Raw OCR Text: $ocrText
            
            CRITICAL INSTRUCTIONS:
            - The user is NOT required to provide code. They are only explaining their logic, time complexity, and space complexity. Do NOT reject them for missing actual code.
            - Be EXTREMELY lenient with passing. As long as they attempt to explain a valid approach or mention relevant data structures/algorithms, you MUST PASS them.
            - Only fail them if their explanation is complete gibberish, spam, or entirely irrelevant.
            - IGNORE any LeetCode UI artifacts in the raw OCR text, such as "Unlock the Full LeetCode Experience", "Subscribe", "Premium", or "testcases passed". Focus ONLY on the problem description and the user's explanation.
            
            Regardless of pass/fail, provide constructive feedback on their explanation as if you were interviewing them. Tell them what they did well and how they could communicate their approach more clearly, concisely, or optimally in a real interview.
            
            IMPORTANT: Format your `interview_feedback` string using rich Markdown (e.g. **bold**, bullet points, code blocks) so it is easy to read.
            
            Return ONLY a valid JSON object matching exactly this schema:
            {
              "pass": true/false,
              "detected_problem_number": "string or null",
              "reason": "short reason for pass/fail",
              "interview_feedback": "Detailed constructive feedback on their communication and solution formatted in Markdown."
            }
        """.trimIndent()

        val rootNode = mutableMapOf<String, Any>(
            "model" to config.model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to explanation)
            ),
            "temperature" to 1.0,
            "top_p" to 0.95,
            "max_tokens" to 1024,
            "stream" to false
        )

        val jsonRequestBody = gson.toJson(rootNode)

        val baseUrl = config.baseUrl.trimEnd('/')
        val finalUrl = if (baseUrl.endsWith("/chat/completions", ignoreCase = true)) {
            baseUrl
        } else {
            "$baseUrl/chat/completions"
        }

        val request = Request.Builder()
            .url(finalUrl)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(jsonRequestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        
        continuation.invokeOnCancellation { call.cancel() }
        
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!continuation.isActive) return
                try {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        throw IOException("HTTP ${response.code}: $errorBody")
                    }
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    
                    val rootNode = gson.fromJson(body, Map::class.java)
                    val choices = rootNode["choices"] as? List<Map<String, Any>>
                    val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
                    val content = message?.get("content")?.toString() ?: throw IOException("No content")
                    
                    val reasoning = message["reasoning"]?.toString() ?: message["reasoning_content"]?.toString()
                    if (reasoning != null) {
                        android.util.Log.d("LlmValidator", "DeepSeek Reasoning: $reasoning")
                    }
                    
                    // Find the first { and last } to extract the JSON object robustly
                    val startIndex = content.indexOf('{')
                    val endIndex = content.lastIndexOf('}')
                    val cleanContent = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                        content.substring(startIndex, endIndex + 1)
                    } else {
                        content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    }
                    
                    val validationResult = gson.fromJson(cleanContent, LlmResponseSchema::class.java)
                    continuation.resume(validationResult)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}
