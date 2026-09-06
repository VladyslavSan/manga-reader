package app.panelrelay

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Serializable
internal data class ReaderSettings(
    val horizontal: Boolean = false,
    val pageWidth: Float = 1f,
    val sidebarVisible: Boolean = true,
    val hideToolbars: Boolean = false,
    val hideSidebar: Boolean = false,
    val checkForUpdates: Boolean = true,
) {
    fun normalized() = copy(pageWidth = if (pageWidth.isFinite()) pageWidth.coerceIn(.25f, 1f) else 1f)
}

internal class DesktopSettingsStore(private val root: Path) {
    private val file = root.resolve("settings.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): ReaderSettings = if (!Files.exists(file)) ReaderSettings() else {
        runCatching { json.decodeFromString<ReaderSettings>(Files.readString(file)).normalized() }
            .getOrDefault(ReaderSettings())
    }

    // Save each explicit preference change before returning, including immediately before quit.
    fun save(settings: ReaderSettings) {
        Files.createDirectories(root)
        val temporary = Files.createTempFile(root, "settings-", ".part")
        try {
            Files.writeString(temporary, json.encodeToString(settings.normalized()))
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
