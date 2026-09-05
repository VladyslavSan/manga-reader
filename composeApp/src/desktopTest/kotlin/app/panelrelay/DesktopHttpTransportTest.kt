package app.panelrelay

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopHttpTransportTest {
    @Test
    fun postsFormContentAndCarriesSessionCookie() = runTest {
        val receivedBody = AtomicReference("")
        val receivedCookie = AtomicReference("")
        val receivedContentType = AtomicReference("")
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/session") { exchange ->
            exchange.responseHeaders.add("Set-Cookie", "panel_session=ready; Path=/")
            exchange.sendResponseHeaders(200, 2)
            exchange.responseBody.use { it.write("ok".encodeToByteArray()) }
        }
        server.createContext("/ajax") { exchange ->
            receivedBody.set(exchange.requestBody.use { it.readBytes().decodeToString() })
            receivedCookie.set(exchange.requestHeaders.getFirst("Cookie").orEmpty())
            receivedContentType.set(exchange.requestHeaders.getFirst("Content-Type").orEmpty())
            exchange.sendResponseHeaders(200, 2)
            exchange.responseBody.use { it.write("ok".encodeToByteArray()) }
        }
        server.start()

        val transport = DesktopHttpTransport()
        try {
            val baseUrl = "http://127.0.0.1:${server.address.port}"
            assertEquals(200, transport.request("$baseUrl/session").status)
            val response = transport.request(
                "$baseUrl/ajax",
                method = "POST",
                body = "action=show&news_id=63031",
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"),
            )

            assertEquals(200, response.status)
            assertEquals("action=show&news_id=63031", receivedBody.get())
            assertTrue(receivedCookie.get().contains("panel_session=ready"))
            assertTrue(receivedContentType.get().startsWith("application/x-www-form-urlencoded"))
        } finally {
            transport.close()
            server.stop(0)
        }
    }
}
