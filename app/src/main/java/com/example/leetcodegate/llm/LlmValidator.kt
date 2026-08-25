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
    val reason: String
)

class LlmValidator(private val client: OkHttpClient, private val gson: Gson) {

    suspend fun validateExplanation(
        config: LlmConfig,
        problemId: String,
        explanation: String,
        ocrText: String
    ): LlmResponseSchema = suspendCancellableCoroutine { continuation ->

        val systemPrompt = """
            You are a strict LeetCode grader. The user wants to unlock Instagram by explaining how they solved a LeetCode problem.
            Evaluate if their explanation is logically sound and proves they solved it, not just gibberish.
            
            Target Problem ID: $problemId
            Raw OCR Text: $ocrText
            
            Return ONLY a valid JSON object matching exactly this schema:
            {
              "pass": true/false,
              "detected_problem_number": "string or null",
              "reason": "short explanation"
            }
        """.trimIndent()

        val jsonRequestBody = """
            {
                "model": "${config.model}",
                "messages": [
                    {"role": "system", "content": ${gson.toJson(systemPrompt)}},
                    {"role": "user", "content": ${gson.toJson(explanation)}}
                ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
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
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    
                    val rootNode = gson.fromJson(body, Map::class.java)
                    val choices = rootNode["choices"] as? List<Map<String, Any>>
                    val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
                    val content = message?.get("content")?.toString() ?: throw IOException("No content")
                    
                    // Strip markdown code blocks in case the LLM wrapped the JSON
                    val cleanContent = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    
                    val validationResult = gson.fromJson(cleanContent, LlmResponseSchema::class.java)
                    continuation.resume(validationResult)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}
