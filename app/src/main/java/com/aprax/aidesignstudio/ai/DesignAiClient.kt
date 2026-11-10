package com.aprax.aidesignstudio.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DesignAiClient(private val settings: ProviderSettings) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchModels(): List<String> {
        val provider = settings.provider
        val key = activeKey()
        require(key.isNotBlank()) { "Save an API key first." }
        val request = if (provider == "Gemini") {
            Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$key").get().build()
        } else {
            Request.Builder().url("https://openrouter.ai/api/v1/models")
                .header("Authorization", "Bearer $key").get().build()
        }
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Model request failed (${response.code}): ${errorMessage(body)}")
            val json = JSONObject(body)
            val array = if (provider == "Gemini") json.optJSONArray("models") ?: JSONArray() else json.optJSONArray("data") ?: JSONArray()
            val result = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = if (provider == "Gemini") item.optString("name") else item.optString("id")
                if (name.isNotBlank() && (provider != "Gemini" || item.optJSONArray("supportedGenerationMethods")?.toString()?.contains("generateContent") == true)) {
                    result += name.removePrefix("models/")
                }
            }
            return result.distinct().sorted()
        }
    }

    fun testConnection(): String {
        val models = fetchModels()
        return "Connected. ${models.size} models available."
    }

    fun generateDesign(prompt: String, category: String, style: String): String {
        val instruction = """You are a professional product designer. Return ONLY valid JSON, no markdown. Build an editable design document for: $category. Style: $style. User brief: $prompt
Schema: {\"title\":\"...\",\"category\":\"...\",\"theme\":{\"background\":\"#F7F6F2\",\"surface\":\"#FFFFFF\",\"text\":\"#202321\",\"muted\":\"#777C78\",\"accent\":\"#6558C9\"},\"pages\":[{\"name\":\"Home\",\"width\":390,\"height\":844,\"elements\":[{\"id\":\"e1\",\"type\":\"heading\",\"text\":\"...\",\"x\":20,\"y\":28,\"width\":350,\"height\":54,\"fontSize\":28,\"color\":\"#202321\",\"background\":\"#FFFFFF\"}]}]}. Create 6-12 meaningful elements. Use only element types heading,text,button,card,image,divider,input,nav. Every element must have id,type,text,x,y,width,height,fontSize,color,background. Use sensible dimensions and concise real copy. Ensure valid JSON and double quotes."""
        val key = activeKey()
        require(key.isNotBlank()) { "Configure an API key in Settings before generating." }
        val provider = settings.provider
        val model = settings.model.ifBlank { if (provider == "Gemini") "gemini-2.0-flash" else "openai/gpt-4o-mini" }
        val payload = if (provider == "Gemini") {
            JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction)))))
                .put("generationConfig", JSONObject().put("temperature", 0.7).put("responseMimeType", "application/json"))
        } else {
            JSONObject().put("model", model)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", instruction)))
                .put("temperature", 0.7)
        }
        val url = if (provider == "Gemini") {
            "https://generativelanguage.googleapis.com/v1beta/models/${model.removePrefix("models/")}:generateContent?key=$key"
        } else "https://openrouter.ai/api/v1/chat/completions"
        val builder = Request.Builder().url(url).post(payload.toString().toRequestBody("application/json".toMediaType()))
        if (provider == "OpenRouter") builder.header("Authorization", "Bearer $key").header("HTTP-Referer", "https://designora.app").header("X-Title", "Designora Studio")
        http.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Generation failed (${response.code}): ${errorMessage(body)}")
            val json = JSONObject(body)
            val text = if (provider == "Gemini") {
                json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
            } else {
                json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            }.orEmpty()
            val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val design = JSONObject(clean)
            require(design.optJSONArray("pages")?.length()?.let { it > 0 } == true) { "AI returned an empty design. Try again with a more detailed prompt." }
            return design.toString()
        }
    }

    private fun activeKey() = if (settings.provider == "Gemini") settings.geminiKey else settings.openRouterKey
    private fun errorMessage(body: String): String = try {
        val root = JSONObject(body)
        root.optJSONObject("error")?.optString("message") ?: root.optString("message", body.take(240))
    } catch (_: Exception) { body.take(240) }
}
