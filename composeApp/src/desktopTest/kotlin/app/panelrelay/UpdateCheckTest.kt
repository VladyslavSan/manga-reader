package app.panelrelay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateCheckTest {
    // jpackage sets this property on every launcher it builds. Getting the name wrong
    // is invisible: currentVersion() returns null, the check short-circuits, and the
    // banner silently never appears.
    @Test fun readsTheVersionJpackageSets() {
        val previous = System.getProperty("jpackage.app-version")
        try {
            System.setProperty("jpackage.app-version", "1.4.2")
            assertEquals("1.4.2", currentVersion())
            System.setProperty("jpackage.app-version", "")
            assertNull(currentVersion())
            System.clearProperty("jpackage.app-version")
            assertNull(currentVersion())
        } finally {
            if (previous == null) System.clearProperty("jpackage.app-version")
            else System.setProperty("jpackage.app-version", previous)
        }
    }

    @Test fun offersOnlyStrictlyNewerVersions() {
        assertTrue(isNewer("v1.1.0", "1.0.0"))
        assertTrue(isNewer("v1.0.1", "1.0.0"))
        assertTrue(isNewer("v2.0.0", "1.9.9"))
        assertFalse(isNewer("v1.0.0", "1.0.0"))
        assertFalse(isNewer("v0.9.9", "1.0.0"))
    }

    @Test fun comparesComponentsNumerically() {
        // Compared as text, "1.10.0" sorts below "1.9.0".
        assertTrue(isNewer("v1.10.0", "1.9.0"))
        assertFalse(isNewer("v1.9.0", "1.10.0"))
        assertTrue(isNewer("v1.0.10", "1.0.9"))
    }

    @Test fun treatsMissingComponentsAsZero() {
        assertFalse(isNewer("v1.2", "1.2.0"))
        assertTrue(isNewer("v1.2.1", "1.2"))
        assertFalse(isNewer("v1.2", "1.2.1"))
    }

    @Test fun staysSilentOnVersionsItCannotRead() {
        assertFalse(isNewer("nightly", "1.0.0"))
        assertFalse(isNewer("v1.0.0-rc1", "1.0.0"))
        assertFalse(isNewer("v1.0.0", "unknown"))
        assertFalse(isNewer("", "1.0.0"))
    }
}
