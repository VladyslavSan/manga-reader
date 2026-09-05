package app.panelrelay

import app.panelrelay.core.LibrarySnapshot
import app.panelrelay.core.LibraryStore
import app.panelrelay.core.MangaPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class DesktopLibraryStore(val root: Path = Path.of(System.getProperty("user.home"), ".panel-relay")) : LibraryStore {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val stateFile get() = root.resolve("library.json")

    override suspend fun load() = withContext(Dispatchers.IO) {
        Files.createDirectories(root)
        if (!stateFile.exists()) LibrarySnapshot()
        else runCatching { json.decodeFromString<LibrarySnapshot>(Files.readString(stateFile)) }.getOrDefault(LibrarySnapshot())
    }

    override suspend fun save(snapshot: LibrarySnapshot) = withContext(Dispatchers.IO) {
        Files.createDirectories(root)
        val temporary = root.resolve("library.json.part")
        Files.writeString(temporary, json.encodeToString(snapshot))
        moveAtomically(temporary, stateFile)
    }

    override suspend fun hasPage(seriesId: String, chapterId: String, page: MangaPage) = withContext(Dispatchers.IO) {
        val file = pageFile(seriesId, chapterId, page)
        file.exists() && runCatching { file.fileSize() > 64 }.getOrDefault(false)
    }

    override suspend fun readPage(seriesId: String, chapterId: String, page: MangaPage) = withContext(Dispatchers.IO) {
        val file = pageFile(seriesId, chapterId, page)
        if (file.exists() && file.fileSize() > 64) file.readBytes() else null
    }

    override suspend fun writePage(seriesId: String, chapterId: String, page: MangaPage, bytes: ByteArray) = withContext(Dispatchers.IO) {
        writeAtomic(pageFile(seriesId, chapterId, page), bytes)
    }

    override suspend fun readCover(seriesId: String) = withContext(Dispatchers.IO) {
        val file = coverFile(seriesId)
        if (file.exists() && file.fileSize() > 64) file.readBytes() else null
    }

    override suspend fun writeCover(seriesId: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        writeAtomic(coverFile(seriesId), bytes)
    }

    fun pageFile(seriesId: String, chapterId: String, page: MangaPage): Path {
        val extension = Regex("""\.([a-zA-Z0-9]{2,5})(?:\?.*)?$""").find(page.url)?.groupValues?.get(1)?.lowercase() ?: "jpg"
        return root.resolve("pages").resolve(safe(seriesId)).resolve(safe(chapterId))
            .resolve(page.index.toString().padStart(4, '0') + "." + extension)
    }

    fun coverFile(seriesId: String) = root.resolve("covers").resolve(safe(seriesId) + ".image")
    private fun safe(value: String) = value.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private fun writeAtomic(destination: Path, bytes: ByteArray) {
        Files.createDirectories(destination.parent)
        val temporary = destination.resolveSibling("${destination.fileName}.part")
        temporary.writeBytes(bytes)
        moveAtomically(temporary, destination)
    }

    private fun moveAtomically(source: Path, destination: Path) {
        runCatching { Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            .getOrElse { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING) }
    }
}

