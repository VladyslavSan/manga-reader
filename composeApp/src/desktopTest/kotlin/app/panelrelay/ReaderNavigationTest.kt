package app.panelrelay

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderNavigationTest {
    @Test fun mapsVerticalAndHorizontalKeys() {
        assertEquals(ReaderNavigationAction.SmallForward, readerNavigationAction(Key.DirectionDown, false))
        assertEquals(ReaderNavigationAction.ViewportBackward, readerNavigationAction(Key.PageUp, false))
        assertEquals(ReaderNavigationAction.PreviousPage, readerNavigationAction(Key.DirectionLeft, true))
        assertEquals(ReaderNavigationAction.ViewportForward, readerNavigationAction(Key.PageDown, true))
    }
    @Test fun mapsSpaceAndChapterBoundariesInBothModes() {
        assertEquals(ReaderNavigationAction.ViewportForward, readerNavigationAction(Key.Spacebar, false))
        assertEquals(ReaderNavigationAction.ViewportBackward, readerNavigationAction(Key.Spacebar, false, shift = true))
        assertEquals(ReaderNavigationAction.ViewportForward, readerNavigationAction(Key.Spacebar, true))
        assertEquals(ReaderNavigationAction.ViewportBackward, readerNavigationAction(Key.Spacebar, true, shift = true))
        for (horizontal in listOf(false, true)) {
            assertEquals(ReaderNavigationAction.ViewportBackward, readerNavigationAction(Key.PageUp, horizontal))
            assertEquals(ReaderNavigationAction.ViewportForward, readerNavigationAction(Key.PageDown, horizontal))
            assertEquals(ReaderNavigationAction.FirstPage, readerNavigationAction(Key.MoveHome, horizontal))
            assertEquals(ReaderNavigationAction.LastPage, readerNavigationAction(Key.MoveEnd, horizontal))
            assertNull(readerNavigationAction(Key.Tab, horizontal))
            assertNull(readerNavigationAction(Key.Enter, horizontal))
        }
    }
}

