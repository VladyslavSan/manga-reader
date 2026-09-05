package app.panelrelay

import app.panelrelay.core.MangaSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveExporter(private val store: DesktopLibraryStore) {
    suspend fun exportDownloaded(series: MangaSeries, destination: Path) = withContext(Dispatchers.IO) {
        destination.parent?.let(Files::createDirectories)
        var count = 0
        ZipOutputStream(Files.newOutputStream(destination)).use { zip ->
            for (chapter in series.chapters) for (page in chapter.pages) {
                val bytes = store.readPage(series.sourceId, chapter.sourceId, page) ?: continue
                val extension = store.pageFile(series.sourceId, chapter.sourceId, page).fileName.toString().substringAfterLast('.', "jpg")
                zip.putNextEntry(ZipEntry("${chapter.sourceId}/${page.index.toString().padStart(4, '0')}.$extension"))
                zip.write(bytes)
                zip.closeEntry()
                count++
            }
        }
        count
    }
}

