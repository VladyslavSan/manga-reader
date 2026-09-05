package app.panelrelay

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.panelrelay.core.DownloadProgress
import app.panelrelay.core.MangaChapter
import app.panelrelay.core.MangaInUaProvider
import app.panelrelay.core.MangaPage
import app.panelrelay.core.MangaRepository
import app.panelrelay.core.MangaSeries
import app.panelrelay.core.RespectfulHttpTransport
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

private val background = Color(0xFF11100F)
private val surface = Color(0xFF191715)
private val border = Color(0xFF3A3631)
private val accent = Color(0xFFF06543)
private val textMuted = Color(0xFFAAA39A)
private val readGreen = Color(0xFF67C587)
private val offlineBlue = Color(0xFF67AEE8)

fun main() = application {
    val rawHttp = remember { DesktopHttpTransport() }
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)
    Window(
        onCloseRequest = { rawHttp.close(); exitApplication() },
        title = "Panel Relay",
        state = windowState,
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(980, 640) }
        val http = remember { RespectfulHttpTransport(rawHttp) }
        val store = remember { DesktopLibraryStore() }
        val repository = remember { MangaRepository(MangaInUaProvider(http), http, store) }
        MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = background, surface = surface)) {
            ReaderApp(repository, store)
        }
    }
}

@Composable
private fun ReaderApp(repository: MangaRepository, store: DesktopLibraryStore) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var library by remember { mutableStateOf<List<MangaSeries>>(emptyList()) }
    var series by remember { mutableStateOf<MangaSeries?>(null) }
    var chapter by remember { mutableStateOf<MangaChapter?>(null) }
    var pages by remember { mutableStateOf<List<MangaPage>>(emptyList()) }
    var status by remember { mutableStateOf("Ready.") }
    var downloadStatus by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var pauseRequested by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var horizontal by remember { mutableStateOf(false) }
    var pageWidth by remember { mutableStateOf(1f) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(true) }
    var showTools by remember { mutableStateOf(false) }
    var readChapters by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var offlineChapters by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedChapterIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionAnchorId by remember { mutableStateOf<String?>(null) }
    var shiftPressed by remember { mutableStateOf(false) }

    val chapterListState = rememberLazyListState()
    val readerListState = rememberLazyListState()
    val pager = rememberPagerState(pageCount = { pages.size })
    val readerFocus = remember { FocusRequester() }
    val smallStep = with(LocalDensity.current) { 180.dp.toPx() }

    fun syncMetadata() {
        library = repository.snapshot.series
        readChapters = repository.snapshot.readChaptersBySeries
        offlineChapters = repository.snapshot.offlineChaptersBySeries
    }

    fun setRead(seriesId: String, chapterIds: Set<String>, read: Boolean, clearSelection: Boolean = false) {
        if (chapterIds.isEmpty()) return
        val current = readChapters[seriesId].orEmpty()
        readChapters = readChapters + (seriesId to if (read) current + chapterIds else current - chapterIds)
        scope.launch {
            runCatching { repository.setChaptersRead(seriesId, chapterIds, read) }
                .onSuccess {
                    syncMetadata()
                    if (clearSelection) {
                        selectedChapterIds = emptySet()
                        selectionAnchorId = null
                        selectionMode = false
                    }
                    status = "Marked ${chapterIds.size} chapter${if (chapterIds.size == 1) "" else "s"} ${if (read) "read" else "unread"}."
                }
                .onFailure { syncMetadata(); status = it.message ?: "Could not save reading progress." }
        }
    }

    suspend fun selectChapter(selectedSeries: MangaSeries, selectedChapter: MangaChapter) {
        busy = true
        status = "Loading ${selectedChapter.label}…"
        runCatching { repository.ensurePages(selectedSeries.sourceId, selectedChapter.sourceId) }
            .onSuccess { loaded ->
                syncMetadata()
                series = repository.snapshot.series.first { it.sourceId == selectedSeries.sourceId }
                chapter = series!!.chapters.first { it.sourceId == selectedChapter.sourceId }
                pages = loaded
                status = "${loaded.size} pages · saved as you read."
            }
            .onFailure { status = it.message ?: "Chapter loading failed." }
        busy = false
    }

    suspend fun openSeries(selected: MangaSeries) {
        showGallery = false
        selectionMode = false
        selectedChapterIds = emptySet()
        selectionAnchorId = null
        val remembered = repository.snapshot.lastChapterBySeries[selected.sourceId]
            ?: repository.snapshot.lastChapterId.takeIf { repository.snapshot.lastSeriesId == selected.sourceId }
        repository.rememberSeries(selected.sourceId)
        val current = repository.snapshot.series.first { it.sourceId == selected.sourceId }
        series = current
        val target = current.chapters.firstOrNull { it.sourceId == remembered } ?: current.chapters.firstOrNull()
        if (target != null) {
            chapterListState.scrollToItem(current.chapters.indexOf(target))
            selectChapter(current, target)
        } else status = "${current.title} has no chapters."
    }

    fun updateSelection(clickedId: String, selected: Boolean) {
        val ids = series?.chapters?.map { it.sourceId } ?: return
        val update = updateChapterSelection(ids, selectedChapterIds, selectionAnchorId, clickedId, selected, shiftPressed)
        selectedChapterIds = update.selectedIds
        selectionAnchorId = update.anchorId
    }

    fun markThroughCurrent() {
        val currentSeries = series ?: return
        val currentChapter = chapter ?: return
        val index = currentSeries.chapters.indexOfFirst { it.sourceId == currentChapter.sourceId }
        if (index >= 0) setRead(currentSeries.sourceId, currentSeries.chapters.take(index + 1).mapTo(mutableSetOf()) { it.sourceId }, true)
    }

    fun navigate(action: ReaderNavigationAction) {
        scope.launch {
            when (action) {
                ReaderNavigationAction.SmallBackward -> readerListState.animateScrollBy(-smallStep)
                ReaderNavigationAction.SmallForward -> readerListState.animateScrollBy(smallStep)
                ReaderNavigationAction.ViewportBackward -> readerListState.animateScrollBy(-readerListState.layoutInfo.run { viewportEndOffset - viewportStartOffset } * .9f)
                ReaderNavigationAction.ViewportForward -> readerListState.animateScrollBy(readerListState.layoutInfo.run { viewportEndOffset - viewportStartOffset } * .9f)
                ReaderNavigationAction.PreviousPage -> if (pager.currentPage > 0) pager.animateScrollToPage(pager.currentPage - 1)
                ReaderNavigationAction.NextPage -> if (pager.currentPage < pages.lastIndex) pager.animateScrollToPage(pager.currentPage + 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        val saved = repository.initialize()
        syncMetadata()
        series = saved.series.firstOrNull { it.sourceId == saved.lastSeriesId } ?: saved.series.firstOrNull()
        series?.let { current ->
            val remembered = saved.lastChapterBySeries[current.sourceId] ?: saved.lastChapterId
            chapter = current.chapters.firstOrNull { it.sourceId == remembered }
            pages = chapter?.pages.orEmpty()
        }
    }
    LaunchedEffect(showGallery) { if (!showGallery) readerFocus.requestFocus() }
    LaunchedEffect(chapter?.sourceId, pages.size) {
        if (chapter != null) {
            readerListState.scrollToItem(0)
            if (pages.isNotEmpty()) pager.scrollToPage(0)
        }
    }

    if (showAddDialog) AddDialog(
        input = input,
        busy = busy,
        onInput = { input = it },
        onDismiss = { if (!busy) showAddDialog = false },
        onAdd = {
            scope.launch {
                busy = true
                status = "Adding manga and loading its chapter registry…"
                runCatching { repository.register(input.trim()) }
                    .onSuccess { imported ->
                        syncMetadata(); showAddDialog = false; input = ""
                        status = "Added ${imported.title} · ${imported.chapters.size} chapters."
                        openSeries(imported)
                    }
                    .onFailure { status = it.message ?: "Adding manga failed." }
                busy = false
            }
        },
    )

    Row(
        Modifier.fillMaxSize().background(background).onPreviewKeyEvent { event ->
            if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) shiftPressed = event.type == KeyEventType.KeyDown
            false
        }
    ) {
        Sidebar(
            library = library,
            series = series,
            chapter = chapter,
            status = status,
            downloadStatus = downloadStatus,
            downloading = downloading,
            busy = busy,
            showTools = showTools,
            readChapters = readChapters,
            offlineChapters = offlineChapters,
            selectionMode = selectionMode,
            selectedChapterIds = selectedChapterIds,
            chapterListState = chapterListState,
            onAdd = { showAddDialog = true },
            onGallery = { showGallery = true },
            onToggleTools = { showTools = !showTools },
            onOpenSeries = { scope.launch { openSeries(it) } },
            onOpenChapter = { item -> series?.let { scope.launch { selectChapter(it, item) } } },
            onToggleSelectionMode = {
                selectionMode = !selectionMode
                if (!selectionMode) { selectedChapterIds = emptySet(); selectionAnchorId = null }
            },
            onSelect = ::updateSelection,
            onSetOneRead = { item, read -> series?.let { setRead(it.sourceId, setOf(item.sourceId), read) } },
            onSetSelectedRead = { read -> series?.let { setRead(it.sourceId, selectedChapterIds, read, clearSelection = true) } },
            onClearSelection = { selectedChapterIds = emptySet(); selectionAnchorId = null },
            onRefresh = refresh@{
                val active = series ?: return@refresh
                scope.launch {
                    busy = true; status = "Refreshing chapter registry…"
                    runCatching { repository.register(active.url) }
                        .onSuccess { refreshed -> syncMetadata(); series = refreshed; status = "Registry refreshed · ${refreshed.chapters.size} chapters." }
                        .onFailure { status = it.message ?: "Registry refresh failed." }
                    busy = false
                }
            },
            onDownload = download@{
                val active = series ?: return@download
                if (downloading) { pauseRequested = true; downloadStatus = "Pausing after the current request…" }
                else {
                    pauseRequested = false; downloading = true; downloadStatus = "Starting background download…"
                    scope.launch {
                        val id = active.sourceId
                        val result = repository.downloadSeries(id, { pauseRequested }) {
                            progress = it
                            downloadStatus = "${it.chapterIndex}/${it.chapterCount} · ${it.cachedPages} pages · ${it.currentChapter}"
                        }
                        syncMetadata()
                        if (series?.sourceId == id) series = repository.snapshot.series.first { it.sourceId == id }
                        downloadStatus = result.message; downloading = false
                    }
                }
            },
            onPause = { pauseRequested = true; downloadStatus = "Pausing after the current request…" },
            onExport = export@{
                val active = series ?: return@export
                chooseExportFile(active.title)?.let { destination -> scope.launch {
                    busy = true
                    val count = ArchiveExporter(store).exportDownloaded(repository.snapshot.series.first { it.sourceId == active.sourceId }, destination)
                    status = "Exported $count pages to $destination"; busy = false
                } }
            },
        )

        if (showGallery) GalleryPane(library, readChapters, offlineChapters, repository, { scope.launch { openSeries(it) } }, { showAddDialog = true }, Modifier.weight(1f))
        else ReaderPane(
            repository, series, chapter, pages, horizontal, pageWidth, busy, readChapters, offlineChapters,
            readerListState, pager, readerFocus,
            onGallery = { showGallery = true },
            onSetRead = { read -> series?.let { s -> chapter?.let { setRead(s.sourceId, setOf(it.sourceId), read) } } },
            onMarkThrough = ::markThroughCurrent,
            onHorizontal = { horizontal = it },
            onWidth = { pageWidth = it },
            onNavigate = ::navigate,
            onCacheChanged = { syncMetadata() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AddDialog(input: String, busy: Boolean, onInput: (String) -> Unit, onDismiss: () -> Unit, onAdd: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add manga") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Paste a supported series or chapter URL. The chapter registry is saved locally.", color = textMuted)
            OutlinedTextField(input, onInput, label = { Text("Manga URL") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { TextButton(enabled = input.isNotBlank() && !busy, onClick = onAdd) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Sidebar(
    library: List<MangaSeries>, series: MangaSeries?, chapter: MangaChapter?, status: String,
    downloadStatus: String?, downloading: Boolean, busy: Boolean, showTools: Boolean,
    readChapters: Map<String, Set<String>>, offlineChapters: Map<String, Set<String>>,
    selectionMode: Boolean, selectedChapterIds: Set<String>, chapterListState: androidx.compose.foundation.lazy.LazyListState,
    onAdd: () -> Unit, onGallery: () -> Unit, onToggleTools: () -> Unit,
    onOpenSeries: (MangaSeries) -> Unit, onOpenChapter: (MangaChapter) -> Unit,
    onToggleSelectionMode: () -> Unit, onSelect: (String, Boolean) -> Unit,
    onSetOneRead: (MangaChapter, Boolean) -> Unit, onSetSelectedRead: (Boolean) -> Unit,
    onClearSelection: () -> Unit, onRefresh: () -> Unit, onDownload: () -> Unit,
    onPause: () -> Unit, onExport: () -> Unit,
) {
    Column(
        Modifier.width(370.dp).fillMaxHeight().background(Color(0xFF171513)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LIBRARY", color = accent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row { TextButton(onClick = onGallery) { Text("Gallery") }; TextButton(enabled = !busy, onClick = onAdd) { Text("+ Add") } }
        }
        if (library.isNotEmpty()) LazyColumn(Modifier.fillMaxWidth().heightIn(max = 76.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            itemsIndexed(library, key = { _, it -> "${it.providerId}:${it.sourceId}" }) { _, item ->
                val selected = item.sourceId == series?.sourceId
                Row(
                    Modifier.fillMaxWidth().background(if (selected) Color(0xFF2D211B) else surface, RoundedCornerShape(6.dp))
                        .clickable(enabled = !busy) { onOpenSeries(item) }.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.title, Modifier.weight(1f), color = if (selected) accent else Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${item.chapters.size}", color = textMuted, fontSize = 11.sp)
                }
            }
        }
        series?.let {
            OutlinedButton(onClick = onToggleTools, modifier = Modifier.fillMaxWidth()) { Text(if (showTools) "Hide tools" else "Downloads & tools") }
            if (showTools) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(enabled = !busy && !downloading, onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
                    Button(onClick = onDownload, modifier = Modifier.weight(1f)) { Text(if (downloading) "Pause" else "Download") }
                    OutlinedButton(enabled = !downloading, onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
                }
            }
        }
        if (!status.startsWith("Ready")) Text(status, color = textMuted, fontSize = 11.sp, maxLines = 2)
        downloadStatus?.let {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF17222B), RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("DOWNLOAD · $it", Modifier.weight(1f), color = offlineBlue, fontSize = 10.sp, maxLines = 2)
                if (downloading) TextButton(onClick = onPause) { Text("Pause", fontSize = 11.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(series?.title ?: "No manga selected", Modifier.weight(1f), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            if (series != null) TextButton(onClick = onToggleSelectionMode) { Text(if (selectionMode) "Cancel" else "Select") }
        }
        if (selectionMode) {
            if (selectedChapterIds.isEmpty()) Text("Check one chapter, then Shift-check another.", color = textMuted, fontSize = 10.sp)
            else Row(
                Modifier.fillMaxWidth().background(Color(0xFF20251F), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${selectedChapterIds.size} selected", Modifier.weight(1f), color = readGreen, fontSize = 11.sp)
                TextButton(onClick = { onSetSelectedRead(true) }) { Text("Read", fontSize = 11.sp) }
                TextButton(onClick = { onSetSelectedRead(false) }) { Text("Unread", fontSize = 11.sp) }
                TextButton(onClick = onClearSelection) { Text("Clear", fontSize = 11.sp) }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = chapterListState,
                modifier = Modifier.fillMaxSize().padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(series?.chapters.orEmpty(), key = { _, it -> it.sourceId }) { _, item ->
                    val current = item.sourceId == chapter?.sourceId
                    val read = item.sourceId in readChapters[series?.sourceId].orEmpty()
                    val offline = item.sourceId in offlineChapters[series?.sourceId].orEmpty()
                    val selected = item.sourceId in selectedChapterIds
                    Row(
                        Modifier.fillMaxWidth().background(
                            when { selected -> Color(0xFF243128); current -> Color(0xFF2D211B); else -> Color.Transparent },
                            RoundedCornerShape(5.dp),
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectionMode) Checkbox(checked = selected, onCheckedChange = { onSelect(item.sourceId, it) })
                        Text(
                            (if (read) "✓ " else "") + (if (offline) "↓  " else "") + item.label,
                            Modifier.weight(1f).clickable(enabled = !busy) { onOpenChapter(item) }.padding(horizontal = 8.dp, vertical = 9.dp),
                            color = if (read) readGreen else if (offline) offlineBlue else if (current) accent else textMuted,
                        )
                        if (!selectionMode) TextButton(onClick = { onSetOneRead(item, !read) }) { Text(if (read) "Unread" else "Read", fontSize = 10.sp) }
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(chapterListState), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

@Composable
private fun GalleryPane(
    library: List<MangaSeries>, read: Map<String, Set<String>>, offline: Map<String, Set<String>>,
    repository: MangaRepository, onOpen: (MangaSeries) -> Unit, onAdd: () -> Unit, modifier: Modifier,
) {
    Column(modifier.fillMaxHeight().background(background)) {
        Row(Modifier.fillMaxWidth().background(surface).padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Your manga", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Saved locally", color = textMuted) }
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF160D09))) { Text("+ Add manga") }
        }
        if (library.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OutlinedButton(onClick = onAdd) { Text("Add your first manga") }
        } else LazyVerticalGrid(
            GridCells.Adaptive(190.dp), Modifier.fillMaxSize().padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(library, key = { "${it.providerId}:${it.sourceId}" }) { item ->
                val readCount = item.chapters.count { it.sourceId in read[item.sourceId].orEmpty() }
                val offlineCount = item.chapters.count { it.sourceId in offline[item.sourceId].orEmpty() }
                val fraction = if (item.chapters.isEmpty()) 0f else readCount.toFloat() / item.chapters.size
                Column(
                    Modifier.background(surface, RoundedCornerShape(10.dp)).border(1.dp, border, RoundedCornerShape(10.dp))
                        .clickable { onOpen(item) }.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    CoverImage(repository, item)
                    Text(item.title, fontWeight = FontWeight.Bold, maxLines = 2)
                    LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth().height(5.dp), color = readGreen, trackColor = Color(0xFF34302C))
                    Text("$readCount read · $offlineCount offline · ${item.chapters.size} total", color = textMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CoverImage(repository: MangaRepository, series: MangaSeries) {
    val result by produceState<Result<ImageBitmap?>?>(null, series.sourceId, series.coverUrl) {
        value = runCatching { repository.loadCover(series)?.let { Image.makeFromEncoded(it).toComposeImageBitmap() } }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(.68f).background(Color(0xFF25211E), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
        val cover = result?.getOrNull()
        when {
            result == null -> CircularProgressIndicator(color = accent)
            cover != null -> Image(cover, series.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Text(series.title.take(1).uppercase(), color = accent, fontSize = 42.sp)
        }
    }
}

@Composable
private fun ReaderPane(
    repository: MangaRepository, series: MangaSeries?, chapter: MangaChapter?, pages: List<MangaPage>,
    horizontal: Boolean, width: Float, busy: Boolean, read: Map<String, Set<String>>, offline: Map<String, Set<String>>,
    listState: androidx.compose.foundation.lazy.LazyListState, pager: androidx.compose.foundation.pager.PagerState,
    focus: FocusRequester, onGallery: () -> Unit, onSetRead: (Boolean) -> Unit, onMarkThrough: () -> Unit,
    onHorizontal: (Boolean) -> Unit, onWidth: (Float) -> Unit, onNavigate: (ReaderNavigationAction) -> Unit,
    onCacheChanged: () -> Unit, modifier: Modifier,
) {
    Column(
        modifier.fillMaxHeight().focusRequester(focus).focusable().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val action = readerNavigationAction(event.key, horizontal) ?: return@onPreviewKeyEvent false
            onNavigate(action); true
        }
    ) {
        Column(Modifier.fillMaxWidth().background(surface).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(chapter?.label ?: "No chapter selected", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val seriesId = series?.sourceId
                val chapterId = chapter?.sourceId
                val isRead = seriesId != null && chapterId != null && chapterId in read[seriesId].orEmpty()
                val isOffline = seriesId != null && chapterId != null && chapterId in offline[seriesId].orEmpty()
                OutlinedButton(onClick = onGallery) { Text("Library") }
                if (isOffline) Text("↓ Offline", color = offlineBlue)
                OutlinedButton(enabled = chapter != null, onClick = { onSetRead(!isRead) }) { Text(if (isRead) "Mark unread" else "Mark read") }
                OutlinedButton(enabled = chapter != null, onClick = onMarkThrough) { Text("Mark through here read") }
                OutlinedButton(onClick = { onHorizontal(false) }) { Text("Vertical") }
                OutlinedButton(onClick = { onHorizontal(true) }) { Text("Horizontal") }
                Text("Width ${(width * 100).toInt()}%", color = textMuted, fontSize = 11.sp)
                Slider(width, onWidth, valueRange = .5f..1f, modifier = Modifier.width(170.dp))
            }
        }
        if (pages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(color = accent) else Text("Select a chapter.", color = textMuted)
        } else if (horizontal) {
            LaunchedEffect(pager.currentPage, pages.size, chapter?.sourceId) {
                if (pager.currentPage == pages.lastIndex) onSetRead(true)
            }
            HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
                Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                    PageImage(repository, series!!, chapter!!, pages[index], width, onCacheChanged)
                }
            }
        } else {
            LaunchedEffect(listState, pages.size, chapter?.sourceId) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == pages.lastIndex }
                    .distinctUntilChanged().collect { if (it) onSetRead(true) }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                itemsIndexed(pages, key = { _, it -> it.url }) { _, page ->
                    PageImage(repository, series!!, chapter!!, page, width, onCacheChanged)
                }
            }
        }
    }
}

@Composable
private fun PageImage(
    repository: MangaRepository, series: MangaSeries, chapter: MangaChapter, page: MangaPage,
    width: Float, onCacheChanged: () -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(null, series.sourceId, chapter.sourceId, page.url) {
        value = runCatching {
            val bytes = repository.loadPage(series.sourceId, chapter.sourceId, page)
            onCacheChanged()
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()
    }
    Box(
        Modifier.fillMaxWidth(width).then(if (bitmap == null) Modifier.height(520.dp).background(surface) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it, "Page ${page.index}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
            ?: CircularProgressIndicator(color = accent)
    }
}

private fun chooseExportFile(title: String): Path? {
    val dialog = FileDialog(null as Frame?, "Export downloaded pages", FileDialog.SAVE)
    dialog.file = title.replace(Regex("[<>:\"/\\|?*]"), "_") + ".zip"
    dialog.isVisible = true
    return dialog.directory?.let { directory -> dialog.file?.let { Path.of(directory, it) } }
}
