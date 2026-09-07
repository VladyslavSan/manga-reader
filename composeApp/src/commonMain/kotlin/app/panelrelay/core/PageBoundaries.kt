package app.panelrelay.core

import kotlin.math.abs

/**
 * Splitting a tall image into the manga pages stacked inside it.
 *
 * Scans are often delivered as one image per several pages, so "next page" cannot mean
 * "next image". Neither the image container nor the source records how many pages an
 * image holds, and aspect ratio only works when pages are a predictable shape, so the
 * count has to be found in the pixels.
 *
 * Pages stacked into one scan are equal height, which turns detection into a search over
 * one number: a split into N pages puts its N-1 cuts at fixed fractions of the image, and
 * each candidate N is judged by its worst cut. A cut through artwork has ink; a cut on a
 * seam does not. Larger N is strictly harder to satisfy - every cut of N=2 recurs in N=4 -
 * so the largest N that clears is the answer, and 1 is the answer when none do.
 *
 * Known limits, inherent rather than unfinished:
 * - Pages butted together with no seam leave no signal and report as one page, which is
 *   today's behaviour anyway.
 * - Unequal-height pages in one image are outside the model. They report as one page
 *   rather than being cut in the wrong place.
 */

internal fun luma(argb: Int): Int =
    ((argb shr 16 and 0xFF) * 299 + (argb shr 8 and 0xFF) * 587 + (argb and 0xFF) * 114) / 1000

/**
 * The background level, taken as the most common luma. Scans are mostly paper, so the
 * dominant value is the background whether the page is white or inverted.
 */
internal fun backgroundLuma(rows: List<IntArray>): Int {
    if (rows.isEmpty()) return 255
    val histogram = IntArray(256)
    for (row in rows) for (argb in row) histogram[luma(argb)]++
    var best = 0
    for (value in 1..255) if (histogram[value] > histogram[best]) best = value
    return best
}

/**
 * Fraction of a row that is not background. A seam is near zero; artwork is not.
 * Measuring proportion rather than demanding uniformity keeps noise, compression
 * artifacts and paper texture from hiding a real seam.
 */
internal fun rowInk(row: IntArray, background: Int, tolerance: Int = 24): Float {
    if (row.isEmpty()) return 0f
    var ink = 0
    for (argb in row) if (abs(luma(argb) - background) > tolerance) ink++
    return ink.toFloat() / row.size
}

/**
 * How many equal-height pages the [ink] profile describes.
 *
 * A cut only counts when the blank run it lands in is *wide*: a page seam is the bottom
 * margin of one page plus the top margin of the next, far taller than the gutter between
 * two panel rows. Without that width test a two-page image whose pages each have a
 * mid-page gutter would report as four.
 *
 * [searchFraction] lets a cut drift slightly off the ideal fraction, which absorbs scans
 * whose margins are not perfectly even.
 */
internal fun pageCount(
    ink: FloatArray,
    maxPages: Int = 8,
    maxInk: Float = 0.02f,
    searchFraction: Float = 0.02f,
    minSeamFraction: Float = 0.01f,
): Int {
    if (ink.size < 2) return 1
    for (pages in maxPages downTo 2) {
        val pageHeight = ink.size.toFloat() / pages
        if (pageHeight < 1f) continue
        val window = (pageHeight * searchFraction).toInt().coerceAtLeast(1)
        val minSeam = (pageHeight * minSeamFraction).toInt().coerceAtLeast(2)
        val fits = (1 until pages).all { index ->
            seamWidthNear(ink, (pageHeight * index).toInt(), window, maxInk) >= minSeam
        }
        if (fits) return pages
    }
    return 1
}

/**
 * Height of the widest blank run overlapping [centre] +/- [window], or 0 when every row
 * there carries ink.
 */
private fun seamWidthNear(ink: FloatArray, centre: Int, window: Int, maxInk: Float): Int {
    val from = (centre - window).coerceAtLeast(0)
    val to = (centre + window).coerceAtMost(ink.size - 1)
    var widest = 0
    for (seed in from..to) {
        if (ink[seed] > maxInk) continue
        var start = seed
        while (start > 0 && ink[start - 1] <= maxInk) start--
        var end = seed
        while (end < ink.size - 1 && ink[end + 1] <= maxInk) end++
        widest = maxOf(widest, end - start + 1)
        // The run is fully measured, so resume past it rather than re-walking it.
        if (end >= to) break
    }
    return widest
}

/** Row at which each detected page starts, always beginning with 0. */
internal fun pageStarts(ink: FloatArray, maxPages: Int = 8): List<Int> {
    val pages = pageCount(ink, maxPages)
    return (0 until pages).map { it * ink.size / pages }
}
