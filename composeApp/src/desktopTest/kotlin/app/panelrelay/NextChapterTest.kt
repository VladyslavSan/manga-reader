package app.panelrelay

import app.panelrelay.core.MangaChapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextChapterTest {
    private val chapters = listOf(
        MangaChapter("1", "Ch 1", "u1"),
        MangaChapter("2", "Ch 2", "u2"),
        MangaChapter("3", "Ch 3", "u3"),
    )

    @Test fun advancesInReadingOrder() {
        assertEquals("2", nextChapterAfter(chapters, "1")?.sourceId)
        assertEquals("3", nextChapterAfter(chapters, "2")?.sourceId)
    }

    @Test fun stopsAtTheLastChapter() {
        assertNull(nextChapterAfter(chapters, "3"))
    }

    // indexOfFirst returns -1 when absent; getOrNull(-1 + 1) would hand back chapter one.
    @Test fun returnsNothingForUnknownOrMissingChapter() {
        assertNull(nextChapterAfter(chapters, "nope"))
        assertNull(nextChapterAfter(chapters, null))
        assertNull(nextChapterAfter(emptyList(), "1"))
    }
}
