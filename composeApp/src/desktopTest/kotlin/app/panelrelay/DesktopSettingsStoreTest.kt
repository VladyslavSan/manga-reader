package app.panelrelay

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSettingsStoreTest {
    @Test fun restoresPreferencesInANewStoreInstance() {
        val root = Files.createTempDirectory("reader-settings-test")
        try {
            val settings = ReaderSettings(horizontal = true, pageWidth = .25f, sidebarVisible = false, hideToolbars = true)
            DesktopSettingsStore(root).save(settings)
            assertEquals(settings, DesktopSettingsStore(root).load())
            val updated = settings.copy(pageWidth = .7f)
            DesktopSettingsStore(root).save(updated)
            assertEquals(updated, DesktopSettingsStore(root).load())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun handlesMissingCorruptAndOlderSettings() {
        val root = Files.createTempDirectory("reader-settings-test")
        try {
            val store = DesktopSettingsStore(root)
            assertEquals(ReaderSettings(), store.load())
            Files.writeString(root.resolve("settings.json"), "broken json")
            assertEquals(ReaderSettings(), store.load())
            Files.writeString(root.resolve("settings.json"), """{"pageWidth":0.1,"futureOption":true}""")
            assertEquals(ReaderSettings(pageWidth = .25f), store.load())
            Files.writeString(root.resolve("settings.json"), """{"pageWidth":2.0}""")
            assertEquals(1f, store.load().pageWidth)
        } finally { root.toFile().deleteRecursively() }
    }
}
