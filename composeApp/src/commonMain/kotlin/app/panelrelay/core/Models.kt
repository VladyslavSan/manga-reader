package app.panelrelay.core

import kotlinx.serialization.Serializable

@Serializable
data class MangaPage(val index: Int, val url: String)

@Serializable
data class MangaChapter(
    val sourceId: String,
    val label: String,
    val url: String,
    val number: Double? = null,
    val volume: String? = null,
    val pages: List<MangaPage> = emptyList(),
)

@Serializable
data class MangaSeries(
    val providerId: String,
    val sourceId: String,
    val title: String,
    val url: String,
    val category: String,
    val coverUrl: String? = null,
    val chapters: List<MangaChapter>,
)

@Serializable
data class LibrarySnapshot(
    val series: List<MangaSeries> = emptyList(),
    val lastSeriesId: String? = null,
    val lastChapterId: String? = null,
    val lastChapterBySeries: Map<String, String> = emptyMap(),
    val readChaptersBySeries: Map<String, Set<String>> = emptyMap(),
    val offlineChaptersBySeries: Map<String, Set<String>> = emptyMap(),
)

data class DownloadProgress(
    val chapterIndex: Int,
    val chapterCount: Int,
    val cachedPages: Int,
    val currentChapter: String,
)

data class DownloadResult(val completed: Boolean, val cachedPages: Int, val message: String)

