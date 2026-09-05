package app.panelrelay.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

data class NativeHttpResponse(val status: Int, val headers: Map<String, String>, val body: ByteArray) {
    fun text(): String = body.decodeToString()
    fun header(name: String): String? = headers[name.lowercase()]
}

interface NativeHttpTransport {
    suspend fun request(
        url: String,
        method: String = "GET",
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): NativeHttpResponse
}

class SourcePausedException(message: String) : Exception(message)

class RespectfulHttpTransport(
    private val delegate: NativeHttpTransport,
    private val minimumDelayMs: Long = 1_250,
    private val jitterMs: Long = 500,
) : NativeHttpTransport {
    private val gate = Mutex()
    private var lastRequest: TimeMark? = null

    override suspend fun request(url: String, method: String, body: String?, headers: Map<String, String>) = gate.withLock {
        val requiredDelay = minimumDelayMs + Random.nextLong(jitterMs + 1)
        val elapsed = lastRequest?.elapsedNow()?.inWholeMilliseconds ?: requiredDelay
        if (elapsed < requiredDelay) delay((requiredDelay - elapsed).milliseconds)
        val response = delegate.request(url, method, body, headers)
        lastRequest = TimeSource.Monotonic.markNow()
        when (response.status) {
            403, 429 -> throw SourcePausedException("The source returned HTTP ${response.status}; downloads were paused to protect the source.")
            in 500..599 -> throw SourcePausedException("The source returned HTTP ${response.status}; retry later.")
        }
        response
    }
}

fun NativeHttpResponse.requireSuccess(context: String): NativeHttpResponse {
    if (status !in 200..299) error("$context failed with HTTP $status")
    return this
}

fun NativeHttpResponse.requireImage(): ByteArray {
    requireSuccess("Image request")
    val contentType = header("content-type").orEmpty().lowercase()
    val jpeg = body.size > 3 && body[0] == 0xFF.toByte() && body[1] == 0xD8.toByte()
    val png = body.size > 8 && body[0] == 0x89.toByte() && body.copyOfRange(1, 4).decodeToString() == "PNG"
    val webp = body.size > 12 && body.copyOfRange(0, 4).decodeToString() == "RIFF" && body.copyOfRange(8, 12).decodeToString() == "WEBP"
    check(contentType.startsWith("image/") && (jpeg || png || webp)) {
        "The source returned non-image content; downloads were paused."
    }
    return body
}

