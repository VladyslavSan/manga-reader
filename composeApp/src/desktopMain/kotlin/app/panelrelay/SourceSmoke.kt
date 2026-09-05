package app.panelrelay

import app.panelrelay.core.MangaInUaProvider
import app.panelrelay.core.RespectfulHttpTransport
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    DesktopHttpTransport().use { transport ->
        val provider = MangaInUaProvider(RespectfulHttpTransport(transport))
        val series = provider.loadSeries("https://manga.in.ua/mangas/boyovik/63027-the-greatest-estate-developer.html")
        val pages = provider.loadPages(series.chapters.first())
        println("Loaded '${series.title}' with ${series.chapters.size} chapters; first chapter has ${pages.size} pages")
        check(series.chapters.isNotEmpty() && pages.isNotEmpty())
    }
}
