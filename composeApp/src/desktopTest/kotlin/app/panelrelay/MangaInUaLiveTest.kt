package app.panelrelay

import app.panelrelay.core.MangaInUaProvider
import app.panelrelay.core.RespectfulHttpTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MangaInUaLiveTest {
    @Test fun loadsKnownSeriesAndPageCatalog() = runTest {
        if (System.getenv("PANEL_RELAY_LIVE_TESTS") != "1") return@runTest
        DesktopHttpTransport().use { transport ->
            val provider = MangaInUaProvider(RespectfulHttpTransport(transport))
            val series = provider.loadSeries("https://manga.in.ua/mangas/boyovik/63027-the-greatest-estate-developer.html")
            assertTrue(series.chapters.size >= 200)
            assertTrue(provider.loadPages(series.chapters.first()).isNotEmpty())
        }
    }
}
