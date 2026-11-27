package com.abhi.ganasuno.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Explicit one-shot Sentry envelope; no automatic events, replay, identifiers or breadcrumbs. */
object CrashUpload {
    const val available=true
    fun send(dsn:String,report:String):String {
        val d=Batch19.destination(dsn)?:error("Invalid HTTPS Sentry DSN")
        val safe=Batch19.safeReport(report);require(safe.isNotBlank()){ "No report to send" }
        val id=UUID.randomUUID().toString().replace("-","")
        val time=Instant.now().toString()
        val event=JSONObject().put("event_id",id).put("timestamp",time).put("platform","java").put("level","error").put("logger","ganasuno.manual_crash").put("message",safe).put("release","ganasuno@19.0").toString()
        val header=JSONObject().put("event_id",id).put("sent_at",time)
        val item=JSONObject().put("type","event").put("length",event.toByteArray(Charsets.UTF_8).size)
        val payload="$header\n$item\n$event\n"
        val http=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(20,TimeUnit.SECONDS).build()
        val request=Request.Builder().url(d.endpoint).header("X-Sentry-Auth","Sentry sentry_version=7, sentry_key=${d.key}, sentry_client=ganasuno/19.0").post(payload.toRequestBody("application/x-sentry-envelope".toMediaType())).build()
        http.newCall(request).execute().use{ check(it.isSuccessful){"Report rejected (HTTP ${it.code})"} }
        return id
    }
}
