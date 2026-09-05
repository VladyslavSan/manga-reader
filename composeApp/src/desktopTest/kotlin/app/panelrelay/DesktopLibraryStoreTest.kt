package app.panelrelay

import app.panelrelay.core.LibrarySnapshot
import app.panelrelay.core.MangaPage
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopLibraryStoreTest {
    @Test fun persistsStatePagesAndCovers() = runTest {
        val root = Files.createTempDirectory("panel-relay-test")
        try {
            val store = DesktopLibraryStore(root); val state = LibrarySnapshot(lastSeriesId = "s")
            store.save(state); assertEquals(state, store.load())
            val page = MangaPage(1, "https://img.test/1.webp"); val bytes = ByteArray(128) { it.toByte() }
            store.writePage("s", "c", page, bytes); assertTrue(store.hasPage("s", "c", page)); assertContentEquals(bytes, store.readPage("s", "c", page))
            store.writeCover("s", bytes); assertContentEquals(bytes, store.readCover("s"))
        } finally { root.toFile().deleteRecursively() }
    }
}

