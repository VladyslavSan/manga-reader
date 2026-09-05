package app.panelrelay

import androidx.compose.ui.input.key.Key

internal enum class ReaderNavigationAction { SmallBackward, SmallForward, ViewportBackward, ViewportForward, PreviousPage, NextPage }

internal fun readerNavigationAction(key: Key, horizontal: Boolean) = if (horizontal) {
    when (key) {
        Key.DirectionLeft, Key.DirectionUp, Key.PageUp -> ReaderNavigationAction.PreviousPage
        Key.DirectionRight, Key.DirectionDown, Key.PageDown -> ReaderNavigationAction.NextPage
        else -> null
    }
} else {
    when (key) {
        Key.DirectionUp -> ReaderNavigationAction.SmallBackward
        Key.DirectionDown -> ReaderNavigationAction.SmallForward
        Key.DirectionLeft, Key.PageUp -> ReaderNavigationAction.ViewportBackward
        Key.DirectionRight, Key.PageDown -> ReaderNavigationAction.ViewportForward
        else -> null
    }
}

