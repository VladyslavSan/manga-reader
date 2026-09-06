package app.panelrelay

import androidx.compose.ui.input.key.Key
import app.panelrelay.core.MangaChapter

internal enum class ReaderNavigationAction { SmallBackward, SmallForward, ViewportBackward, ViewportForward, PreviousPage, NextPage, FirstPage, LastPage }

internal fun readerNavigationAction(key: Key, horizontal: Boolean, shift: Boolean = false): ReaderNavigationAction? {
    when (key) {
        Key.MoveHome -> return ReaderNavigationAction.FirstPage
        Key.MoveEnd -> return ReaderNavigationAction.LastPage
        Key.PageUp -> return ReaderNavigationAction.ViewportBackward
        Key.PageDown -> return ReaderNavigationAction.ViewportForward
        Key.Spacebar -> return if (shift) ReaderNavigationAction.ViewportBackward else ReaderNavigationAction.ViewportForward
    }
    return if (horizontal) {
        when (key) {
            Key.DirectionLeft, Key.DirectionUp -> ReaderNavigationAction.PreviousPage
            Key.DirectionRight, Key.DirectionDown -> ReaderNavigationAction.NextPage
            else -> null
        }
    } else {
        when (key) {
            Key.DirectionUp -> ReaderNavigationAction.SmallBackward
            Key.DirectionDown -> ReaderNavigationAction.SmallForward
            Key.DirectionLeft -> ReaderNavigationAction.ViewportBackward
            Key.DirectionRight -> ReaderNavigationAction.ViewportForward
            else -> null
        }
    }
}

/**
 * The chapter after [currentId] in reading order, or null when it is the last one or
 * is not in the list. Chapters arrive sorted ascending by volume then number, so the
 * next one to read is the following element - reversing this would send readers
 * backwards at the end of every chapter.
 */
internal fun nextChapterAfter(chapters: List<MangaChapter>, currentId: String?): MangaChapter? {
    if (currentId == null) return null
    val index = chapters.indexOfFirst { it.sourceId == currentId }
    return if (index < 0) null else chapters.getOrNull(index + 1)
}
