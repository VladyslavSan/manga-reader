package app.panelrelay

import androidx.compose.ui.input.key.Key

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
