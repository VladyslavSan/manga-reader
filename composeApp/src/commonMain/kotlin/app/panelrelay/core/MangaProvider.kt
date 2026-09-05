package app.panelrelay.core

interface MangaProvider {
    val id: String
    fun recognizes(url: String): Boolean
    suspend fun loadSeries(url: String): MangaSeries
    suspend fun loadPages(chapter: MangaChapter): List<MangaPage>
}

