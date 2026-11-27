package com.abhi.ganasuno.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.abhi.ganasuno.data.PlaybackRecovery
import com.abhi.ganasuno.data.StreamLookupFailure

@UnstableApi
object PlaybackFailures {
    data class Failure(val item: String, val code: Int, val name: String, val http: Int?, val lookup: Boolean, val restricted: Boolean) {
        val text: String get() = PlaybackRecovery.description(code, http, lookup, restricted)
        val retryable: Boolean get() = PlaybackRecovery.transient(code, http, restricted)
        fun summary(): String = "$text\nCode: $name ($code)" + (http?.let { " · HTTP $it" } ?: "") + if (lookup) " · Stream lookup" else ""
    }
    @Volatile var last: Failure? = null
    fun inspect(item: String, error: PlaybackException): Failure {
        val causes = generateSequence<Throwable>(error) { it.cause?.takeIf { next -> next !== it } }.take(12).toList()
        val http = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode
        val restricted = causes.any { t ->
            val type = t.javaClass.simpleName.lowercase()
            listOf("recaptcha", "loginrequired", "ageRestricted".lowercase(), "contentnotsupported", "geographicrestriction", "paidcontent", "privatecontent").any { type.contains(it) }
        }
        return Failure(item, error.errorCode, error.errorCodeName, http, causes.any { it is StreamLookupFailure }, restricted)
    }
}
