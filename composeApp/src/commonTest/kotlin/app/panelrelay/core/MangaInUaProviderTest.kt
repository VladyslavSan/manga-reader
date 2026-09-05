package app.panelrelay.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MangaInUaProviderTest {
    @Test fun parsesAndSortsChapters() {
        val provider = MangaInUaProvider(noNetwork())
        val chapters = provider.parseChapters("""
            <a class="chapterscalc" href="/chapters/2-two.html">Том 1. Розділ 2</a>
            <a class="foo chapterscalc bar" href="/chapters/1-one.html">Том 1. Розділ 1</a>
        """.trimIndent())
        assertEquals(listOf("1", "2"), chapters.map { it.sourceId })
    }

    @Test fun usesAjaxContractsForCatalogAndPages() = runTest {
        val calls = mutableListOf<String>()
        val transport = object : NativeHttpTransport {
            override suspend fun request(url: String, method: String, body: String?, headers: Map<String, String>): NativeHttpResponse {
                calls += "$method $url"
                val html = when {
                    "load_chapters_image" in url -> { assertEquals("XMLHttpRequest", headers["X-Requested-With"]); "<img data-src=\"https://img.test/1.webp\">" }
                    "mod=load_chapters" in url -> {
                        assertEquals("POST", method); assertTrue(body.orEmpty().contains("news_id=63027"))
                        "<a class=\"chapterscalc\" href=\"/chapters/70001-one.html\">Том 1. Розділ 1</a>"
                    }
                    "/chapters/" in url -> "<script>var site_login_hash='chapter';</script><div id='comics' data-news_id='70001'></div>"
                    else -> "<script>var site_login_hash='series';</script><div id='linkstocomics' data-news_id='63027' data-news_category='1'></div><span class='UAname'>Test</span>"
                }
                return NativeHttpResponse(200, mapOf("content-type" to "text/html"), html.encodeToByteArray())
            }
        }
        val provider = MangaInUaProvider(transport)
        val series = provider.loadSeries("https://manga.in.ua/mangas/test/63027-test.html")
        assertEquals(1, provider.loadPages(series.chapters.single()).size)
        assertEquals(4, calls.size)
    }

    private fun noNetwork() = object : NativeHttpTransport {
        override suspend fun request(url: String, method: String, body: String?, headers: Map<String, String>) = error("unused")
    }
}

