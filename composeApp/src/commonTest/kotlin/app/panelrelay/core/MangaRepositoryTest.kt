package app.panelrelay.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MangaRepositoryTest {
    @Test fun persistsMultipleTitlesProgressAndRefresh() = runTest {
        val provider = FakeProvider(); val store = MemoryStore(); val repo = MangaRepository(provider, provider.transport, store)
        repo.initialize(); repo.register("https://source.test/one"); repo.ensurePages("one", "one-2")
        repo.register("https://source.test/two"); repo.ensurePages("two", "two-1")
        repo.setChaptersRead("one", setOf("one-1", "one-2"), true)
        provider.extra = true
        val refreshed = repo.register("https://source.test/one")
        assertEquals(2, repo.snapshot.series.size)
        assertEquals(3, refreshed.chapters.size)
        assertTrue(refreshed.chapters.first { it.sourceId == "one-2" }.pages.isNotEmpty())
        assertEquals(setOf("one-1", "one-2"), repo.snapshot.readChaptersBySeries["one"])
    }

    @Test fun concurrentManifestLoadsMerge() = runTest {
        val provider = FakeProvider(); val repo = MangaRepository(provider, provider.transport, MemoryStore())
        repo.initialize(); val series = repo.register("https://source.test/one")
        coroutineScope { series.chapters.map { async { repo.ensurePages("one", it.sourceId) } }.awaitAll() }
        assertTrue(repo.snapshot.series.single().chapters.all { it.pages.isNotEmpty() })
    }

    private class FakeProvider : MangaProvider {
        var extra = false
        override val id = "fake"
        val transport = object : NativeHttpTransport {
            override suspend fun request(url: String, method: String, body: String?, headers: Map<String, String>) = error("unused")
        }
        override fun recognizes(url: String) = url.startsWith("https://source.test/")
        override suspend fun loadSeries(url: String): MangaSeries {
            val id = url.substringAfterLast('/'); val count = if (id == "one" && extra) 3 else if (id == "one") 2 else 1
            return MangaSeries(this.id, id, "Title $id", url, "test", chapters = (1..count).map { MangaChapter("$id-$it", "Chapter $it", "$url/$it") })
        }
        override suspend fun loadPages(chapter: MangaChapter): List<MangaPage> { delay(1); return listOf(MangaPage(1, "https://img.test/${chapter.sourceId}.webp")) }
    }

    private class MemoryStore : LibraryStore {
        var state = LibrarySnapshot(); private val files = mutableSetOf<String>()
        override suspend fun load() = state
        override suspend fun save(snapshot: LibrarySnapshot) { state = snapshot }
        override suspend fun hasPage(seriesId: String, chapterId: String, page: MangaPage) = "$seriesId/$chapterId/${page.index}" in files
        override suspend fun readPage(seriesId: String, chapterId: String, page: MangaPage): ByteArray? = null
        override suspend fun writePage(seriesId: String, chapterId: String, page: MangaPage, bytes: ByteArray) { files += "$seriesId/$chapterId/${page.index}" }
        override suspend fun readCover(seriesId: String): ByteArray? = null
        override suspend fun writeCover(seriesId: String, bytes: ByteArray) = Unit
    }
}
