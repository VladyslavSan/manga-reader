package app.panelrelay

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderNavigationTest {
    @Test fun mapsVerticalAndHorizontalKeys() {
        assertEquals(ReaderNavigationAction.SmallForward, readerNavigationAction(Key.DirectionDown, false))
        assertEquals(ReaderNavigationAction.ViewportBackward, readerNavigationAction(Key.PageUp, false))
        assertEquals(ReaderNavigationAction.PreviousPage, readerNavigationAction(Key.DirectionLeft, true))
        assertEquals(ReaderNavigationAction.NextPage, readerNavigationAction(Key.PageDown, true))
    }
}

