package app.panelrelay.core

interface LibraryStore {
    suspend fun load(): LibrarySnapshot
    suspend fun save(snapshot: LibrarySnapshot)
    suspend fun hasPage(seriesId: String, chapterId: String, page: MangaPage): Boolean
    suspend fun readPage(seriesId: String, chapterId: String, page: MangaPage): ByteArray?
    suspend fun writePage(seriesId: String, chapterId: String, page: MangaPage, bytes: ByteArray)
    suspend fun readCover(seriesId: String): ByteArray?
    suspend fun writeCover(seriesId: String, bytes: ByteArray)
}

class MangaRepository(
    private val provider: MangaProvider,
    private val http: NativeHttpTransport,
    val store: LibraryStore,
) {
    var snapshot = LibrarySnapshot()
        private set

    suspend fun initialize(): LibrarySnapshot {
        snapshot = store.load()
        reconcileOfflineChapters()
        return snapshot
    }

    private suspend fun reconcileOfflineChapters() {
        val reconciled = mutableMapOf<String, Set<String>>()
        for (series in snapshot.series) {
            val offline = mutableSetOf<String>()
            for (chapter in series.chapters) {
                if (chapter.pages.isEmpty()) continue
                var complete = true
                for (page in chapter.pages) {
                    if (!store.hasPage(series.sourceId, chapter.sourceId, page)) { complete = false; break }
                }
                if (complete) offline += chapter.sourceId
            }
            if (offline.isNotEmpty()) reconciled[series.sourceId] = offline
        }
        if (reconciled != snapshot.offlineChaptersBySeries) {
            snapshot = snapshot.copy(offlineChaptersBySeries = reconciled)
            store.save(snapshot)
        }
    }

    suspend fun register(url: String): MangaSeries {
        require(provider.recognizes(url)) { "No provider recognizes this URL." }
        val imported = provider.loadSeries(url)
        val existing = snapshot.series.firstOrNull { it.providerId == imported.providerId && it.sourceId == imported.sourceId }
        val cached = existing?.chapters.orEmpty().associateBy { it.sourceId }
        val merged = imported.copy(chapters = imported.chapters.map { fresh ->
            cached[fresh.sourceId]?.takeIf { it.pages.isNotEmpty() }?.let { fresh.copy(pages = it.pages) } ?: fresh
        })
        snapshot = snapshot.copy(
            series = snapshot.series.filterNot { it.providerId == merged.providerId && it.sourceId == merged.sourceId } + merged,
            lastSeriesId = merged.sourceId,
            lastChapterId = snapshot.lastChapterBySeries[merged.sourceId]
                ?: snapshot.lastChapterId.takeIf { snapshot.lastSeriesId == merged.sourceId },
        )
        store.save(snapshot)
        return merged
    }

    suspend fun rememberSeries(seriesId: String) {
        require(snapshot.series.any { it.sourceId == seriesId }) { "Unknown library title." }
        val remembered = snapshot.lastChapterBySeries[seriesId]
            ?: snapshot.lastChapterId.takeIf { snapshot.lastSeriesId == seriesId }
        snapshot = snapshot.copy(lastSeriesId = seriesId, lastChapterId = remembered)
        store.save(snapshot)
    }

    suspend fun setChapterRead(seriesId: String, chapterId: String, read: Boolean) =
        setChaptersRead(seriesId, setOf(chapterId), read)

    suspend fun setChaptersRead(seriesId: String, chapterIds: Set<String>, read: Boolean) {
        val series = snapshot.series.firstOrNull { it.sourceId == seriesId } ?: error("Unknown library title.")
        val known = series.chapters.mapTo(mutableSetOf()) { it.sourceId }
        require(chapterIds.all { it in known }) { "Unknown chapter." }
        val current = snapshot.readChaptersBySeries[seriesId].orEmpty()
        val updated = if (read) current + chapterIds else current - chapterIds
        if (updated == current) return
        snapshot = snapshot.copy(readChaptersBySeries = snapshot.readChaptersBySeries + (seriesId to updated))
        store.save(snapshot)
    }

    suspend fun openChapter(seriesId: String, chapterId: String): List<MangaPage> {
        val pages = ensurePages(seriesId, chapterId)
        snapshot = rememberChapter(snapshot, seriesId, chapterId)
        store.save(snapshot)
        return pages
    }

    suspend fun ensurePages(seriesId: String, chapterId: String): List<MangaPage> {
        val series = snapshot.series.first { it.sourceId == seriesId }
        val chapter = series.chapters.first { it.sourceId == chapterId }
        if (chapter.pages.isNotEmpty()) return chapter.pages
        val pages = provider.loadPages(chapter)
        val latestSeries = snapshot.series.first { it.sourceId == seriesId }
        val latestChapter = latestSeries.chapters.first { it.sourceId == chapterId }
        val updatedChapter = latestChapter.copy(pages = pages)
        val updatedSeries = latestSeries.copy(chapters = latestSeries.chapters.map {
            if (it.sourceId == chapterId) updatedChapter else it
        })
        snapshot = snapshot.copy(series = snapshot.series.map { if (it.sourceId == seriesId) updatedSeries else it })
        store.save(snapshot)
        return pages
    }

    private fun rememberChapter(current: LibrarySnapshot, seriesId: String, chapterId: String) = current.copy(
        lastSeriesId = seriesId,
        lastChapterId = chapterId,
        lastChapterBySeries = current.lastChapterBySeries + (seriesId to chapterId),
    )

    suspend fun loadPage(seriesId: String, chapterId: String, page: MangaPage): ByteArray {
        store.readPage(seriesId, chapterId, page)?.let {
            updateOfflineIfComplete(seriesId, chapterId, page)
            return it
        }
        val bytes = http.request(page.url).requireImage()
        store.writePage(seriesId, chapterId, page, bytes)
        updateOfflineIfComplete(seriesId, chapterId, page)
        return bytes
    }

    suspend fun loadCover(series: MangaSeries): ByteArray? {
        store.readCover(series.sourceId)?.let { return it }
        val url = series.coverUrl ?: return null
        val bytes = http.request(url).requireImage()
        store.writeCover(series.sourceId, bytes)
        return bytes
    }

    private suspend fun updateOfflineIfComplete(seriesId: String, chapterId: String, loadedPage: MangaPage) {
        if (chapterId in snapshot.offlineChaptersBySeries[seriesId].orEmpty()) return
        val chapter = snapshot.series.first { it.sourceId == seriesId }.chapters.first { it.sourceId == chapterId }
        if (chapter.pages.lastOrNull()?.index != loadedPage.index) return
        if (chapter.pages.isNotEmpty() && chapter.pages.all { store.hasPage(seriesId, chapterId, it) }) {
            snapshot = snapshot.copy(
                offlineChaptersBySeries = snapshot.offlineChaptersBySeries +
                    (seriesId to (snapshot.offlineChaptersBySeries[seriesId].orEmpty() + chapterId))
            )
            store.save(snapshot)
        }
    }

    suspend fun downloadSeries(
        seriesId: String,
        shouldPause: () -> Boolean,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadResult {
        val chapterIds = snapshot.series.first { it.sourceId == seriesId }.chapters.map { it.sourceId }
        var cached = 0
        for ((chapterIndex, chapterId) in chapterIds.withIndex()) {
            if (shouldPause()) return DownloadResult(false, cached, "Paused; all completed files are safe.")
            val chapter = snapshot.series.first { it.sourceId == seriesId }.chapters.first { it.sourceId == chapterId }
            try {
                val pages = ensurePages(seriesId, chapterId)
                for (page in pages) {
                    if (shouldPause()) return DownloadResult(false, cached, "Paused; all completed files are safe.")
                    if (!store.hasPage(seriesId, chapterId, page)) {
                        store.writePage(seriesId, chapterId, page, http.request(page.url).requireImage())
                    }
                    cached++
                    onProgress(DownloadProgress(chapterIndex + 1, chapterIds.size, cached, chapter.label))
                }
                pages.lastOrNull()?.let { updateOfflineIfComplete(seriesId, chapterId, it) }
            } catch (error: SourcePausedException) {
                return DownloadResult(false, cached, error.message ?: "The source requested a pause.")
            } catch (error: Throwable) {
                return DownloadResult(false, cached, "Paused safely: ${error.message ?: error::class.simpleName}")
            }
        }
        return DownloadResult(true, cached, "All $cached pages are available offline.")
    }
}
