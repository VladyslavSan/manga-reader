package app.panelrelay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val LATEST_RELEASE_URL =
    "https://api.github.com/repos/VladyslavSan/manga-reader/releases/latest"

@Serializable
internal data class LatestRelease(
    @SerialName("tag_name") val tag: String = "",
    @SerialName("html_url") val url: String = "",
)

/**
 * The packaged version. jpackage passes this to every launcher it builds, so it always
 * matches packageVersion without the build file arranging anything. Null when running
 * from Gradle, which keeps the update check quiet during development.
 */
internal fun currentVersion(): String? =
    System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() }

/**
 * Compares dotted numeric versions, ignoring a leading "v". Missing components count
 * as zero, so "1.2" equals "1.2.0". A component that is not a number stops the
 * comparison rather than throwing: an unexpected tag reports no update instead of
 * pushing users at a release that may not exist.
 */
internal fun isNewer(candidate: String, current: String): Boolean {
    val latest = versionParts(candidate) ?: return false
    val running = versionParts(current) ?: return false
    for (i in 0 until maxOf(latest.size, running.size)) {
        val a = latest.getOrElse(i) { 0 }
        val b = running.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}

private fun versionParts(version: String): List<Int>? =
    version.removePrefix("v").split(".").map { it.toIntOrNull() ?: return null }

/**
 * Reads the newest published release. Returns null on any failure - being offline,
 * rate limited, or seeing a shape we do not recognise are all reasons to say nothing
 * rather than interrupt the reader.
 */
internal suspend fun fetchLatestRelease(): LatestRelease? = withContext(Dispatchers.IO) {
    runCatching {
        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        val request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_URL))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "manga-reader")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return@runCatching null
        Json { ignoreUnknownKeys = true }
            .decodeFromString<LatestRelease>(response.body())
            .takeIf { it.tag.isNotBlank() && it.url.isNotBlank() }
    }.getOrNull()
}
