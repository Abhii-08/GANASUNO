package com.abhi.ganasuno.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Explicit opt-in, request-scoped credentials. No key, prompt or response is logged or stored. */
object AiTools {
    fun generate(provider: Int, endpoint: String, model: String, key: String, prompt: String, customHeaders:String=""): String {
        require((key.isNotBlank()||provider==2) && model.matches(Regex("[A-Za-z0-9._:/-]{1,150}"))) { "Enter a valid model and API key" }
        require(prompt.length in 1..24000) { "Input too long" }
        val json = "application/json".toMediaType()
        val req = Request.Builder()
        if (provider == 1) {
            require(model.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Enter a Gemini model ID" }
            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            req.url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").header("x-goog-api-key", key).post(body.toString().toRequestBody(json))
        } else {
            val base = (if (provider == 0) "https://api.openai.com/v1/chat/completions" else endpoint.trim()).toHttpUrlOrNull() ?: error("Invalid HTTPS endpoint")
            require(base.isHttps && base.username.isEmpty() && base.password.isEmpty()) { "Use an HTTPS endpoint without embedded credentials" }
            val body = JSONObject().put("model", model).put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            req.url(base).post(body.toString().toRequestBody(json));if(key.isNotBlank())req.header("Authorization", "Bearer $key");if(provider==2)BatchCore.headers(customHeaders).forEach{(name,value)->req.header(name,value)}
        }
        val client = YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(90, TimeUnit.SECONDS).build()
        client.newCall(req.build()).execute().use { r ->
            check(r.isSuccessful) { "Provider returned HTTP ${r.code}" }
            val raw = JSONObject(r.body?.string() ?: error("Empty response"))
            val answer = if (provider == 1) {
                val parts = raw.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                (0 until parts.length()).joinToString("\n") { parts.getJSONObject(it).optString("text") }
            } else raw.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            return answer.trim().take(30000).ifBlank { error("No text returned") }
        }
    }
}
