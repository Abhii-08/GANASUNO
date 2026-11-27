package com.abhi.ganasuno.data

import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import okhttp3.Request as OkRequest

/** HTTP layer the extractor needs (same idea as SimpMusic's BraveNewPipeDownloaderImpl). */
class NpDownloader : Downloader() {
    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val url = request.url()
        val data = request.dataToSend()

        val body =
            when {
                data != null -> data.toRequestBody()
                method == "POST" || method == "PUT" || method == "PATCH" -> ByteArray(0).toRequestBody()
                else -> null
            }

        val builder =
            OkRequest
                .Builder()
                .method(method, body)
                .url(url)
                .addHeader("User-Agent", YtApi.WEB_UA)

        request.headers().forEach { (name, values) ->
            if (values.size > 1) {
                builder.removeHeader(name)
                values.forEach { value -> builder.addHeader(name, value) }
            } else if (values.size == 1) {
                builder.header(name, values[0])
            }
        }

        YtApi.http.newCall(builder.build()).execute().use { response ->
            if (response.code >= 400) {
                // Record status only; request URL, credentials and response body stay private.
                Diagnostics.event("Stream extractor HTTP ${response.code}")
                android.util.Log.w("GANASUNO-Playback", "Stream extractor HTTP ${response.code}")
            }
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }
            val text = response.body?.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                text,
                response.request.url.toString(),
            )
        }
    }
}
