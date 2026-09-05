package app.panelrelay

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderSelectionTest {
    @Test fun selectsInclusiveRangesInEitherDirection() {
        val ids = listOf("1", "2", "3", "4", "5")
        val forward = updateChapterSelection(ids, setOf("2"), "2", "5", true, true)
        assertEquals(setOf("2", "3", "4", "5"), forward.selectedIds)
        val reverse = updateChapterSelection(ids, emptySet(), "5", "2", true, true)
        assertEquals(setOf("2", "3", "4", "5"), reverse.selectedIds)
    }
}

