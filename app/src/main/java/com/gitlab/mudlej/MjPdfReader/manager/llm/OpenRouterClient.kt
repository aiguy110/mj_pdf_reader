package com.gitlab.mudlej.MjPdfReader.manager.llm

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterHttpException(val code: Int, message: String) : Exception(message)

data class PageBlock(val pageText: String, val imageBase64Jpeg: String)

private data class ImageUrl(val url: String)
private data class ContentPart(val type: String, val text: String? = null, val image_url: ImageUrl? = null)
private data class ReqMessage(val role: String, val content: List<ContentPart>)
private data class ChatRequest(
    val model: String,
    val messages: List<ReqMessage>,
    val temperature: Double,
    val max_tokens: Int,
)

private data class RespMessage(val content: String)
private data class Choice(val message: RespMessage)
private data class ChatCompletionResponse(val choices: List<Choice>)

class OpenRouterClient {

    companion object {
        private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODEL = "qwen/qwen3.6-27b"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private val httpClient = OkHttpClient.Builder()
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    // Throws IOException on network failure, OpenRouterHttpException on non-2xx response.
    fun requestAnalysis(apiKey: String, systemPrompt: String, pages: List<PageBlock>): String {
        val userContent = mutableListOf<ContentPart>()
        pages.forEachIndexed { index, page ->
            userContent.add(ContentPart(type = "text", text = "Page ${index + 1} text:\n${page.pageText}"))
            userContent.add(
                ContentPart(
                    type = "image_url",
                    image_url = ImageUrl(url = "data:image/jpeg;base64,${page.imageBase64Jpeg}")
                )
            )
        }

        val requestBody = ChatRequest(
            model = MODEL,
            messages = listOf(
                ReqMessage(role = "system", content = listOf(ContentPart(type = "text", text = systemPrompt))),
                ReqMessage(role = "user", content = userContent),
            ),
            temperature = 0.1,
            max_tokens = 2000,
        )

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(requestBody).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw OpenRouterHttpException(response.code, bodyString)
            }
            val parsed = gson.fromJson(bodyString, ChatCompletionResponse::class.java)
            return parsed.choices.firstOrNull()?.message?.content
                ?: throw IOException("OpenRouter response contained no choices")
        }
    }
}
