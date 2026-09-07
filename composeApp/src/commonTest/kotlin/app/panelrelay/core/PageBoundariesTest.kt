package app.panelrelay.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Profiles are built the way a real one is measured: mostly inked artwork with blank
 * bands where the pages meet. [seam] is generous, [gutter] is the thin blank strip
 * between two panel rows that must not be mistaken for a page break.
 */
private fun profile(height: Int, seams: List<Int>, seam: Int = 24, ink: Float = .5f): FloatArray {
    val values = FloatArray(height) { ink }
    for (centre in seams) {
        for (y in (centre - seam / 2) until (centre + seam / 2)) if (y in values.indices) values[y] = 0f
    }
    return values
}

class PageBoundariesTest {
    @Test
    fun readsLumaFromArgb() {
        assertEquals(255, luma(0xFFFFFFFF.toInt()))
        assertEquals(0, luma(0xFF000000.toInt()))
    }

    @Test
    fun takesTheDominantLumaAsBackground() {
        val paper = IntArray(10) { 0xFFF2F2F2.toInt() }
        val mixed = IntArray(10) { if (it < 3) 0xFF101010.toInt() else 0xFFF2F2F2.toInt() }
        assertEquals(luma(0xFFF2F2F2.toInt()), backgroundLuma(listOf(paper, mixed)))
    }

    @Test
    fun measuresInkAsAFractionOfTheRow() {
        val background = 240
        val row = IntArray(10) { if (it < 2) 0xFF000000.toInt() else 0xFFF0F0F0.toInt() }
        assertEquals(.2f, rowInk(row, background))
    }

    @Test
    fun countsASinglePageWhenNothingSplitsIt() {
        assertEquals(1, pageCount(profile(3000, emptyList())))
    }

    @Test
    fun findsThreeEqualPages() {
        assertEquals(3, pageCount(profile(3000, listOf(1000, 2000))))
    }

    @Test
    fun findsSixEqualPages() {
        assertEquals(6, pageCount(profile(6000, (1..5).map { it * 1000 })))
    }

    @Test
    fun prefersTheFinerSplitWhenEveryCutIsClean() {
        // Cuts for two pages are a subset of the cuts for four, so four must win.
        assertEquals(4, pageCount(profile(4000, listOf(1000, 2000, 3000))))
    }

    @Test
    fun ignoresPanelGuttersInsidePages() {
        // Two real seams plus thin gutters exactly where a four-way split would cut.
        val values = profile(4000, listOf(2000))
        for (centre in listOf(1000, 3000)) for (y in (centre - 2) until (centre + 2)) values[y] = 0f
        assertEquals(2, pageCount(values))
    }

    @Test
    fun toleratesSeamsSlightlyOffTheIdealFraction() {
        assertEquals(3, pageCount(profile(3000, listOf(1012, 1990))))
    }

    @Test
    fun rejectsASplitWhenOneSeamIsMissing() {
        // Three-way spacing but only one of the two seams is present.
        assertEquals(1, pageCount(profile(3000, listOf(1000))))
    }

    @Test
    fun leavesUnequalStacksAsOnePage() {
        assertEquals(1, pageCount(profile(3000, listOf(700))))
    }

    @Test
    fun reportsWhereEachPageStarts() {
        assertEquals(listOf(0, 1000, 2000), pageStarts(profile(3000, listOf(1000, 2000))))
        assertEquals(listOf(0), pageStarts(profile(3000, emptyList())))
    }
}
