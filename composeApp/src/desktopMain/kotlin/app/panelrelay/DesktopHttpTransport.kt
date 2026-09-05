package app.panelrelay

import app.panelrelay.core.NativeHttpResponse
import app.panelrelay.core.NativeHttpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DesktopHttpTransport : NativeHttpTransport, Closeable {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private val client = HttpClient.newBuilder().cookieHandler(cookies)
        .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(45)).build()

    override suspend fun request(url: String, method: String, body: String?, headers: Map<String, String>) =
        withContext(Dispatchers.IO) {
            val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45))
                .header("User-Agent", "Panel Relay/0.1 (personal offline reader)")
                .header("Accept-Language", "uk,en;q=0.8")
            headers.forEach { (name, value) -> builder.header(name, value) }
            val publisher = body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody()
            val response = client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofByteArray())
            NativeHttpResponse(
                response.statusCode(),
                response.headers().map().mapValues { (_, values) -> values.joinToString(",") },
                response.body(),
            )
        }

    override fun close() = Unit
}

