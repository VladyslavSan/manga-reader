package app.panelrelay

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onSizeChanged
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
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.net.URI
import java.nio.file.Path

private val background = Color(0xFF11100F)
private val surface = Color(0xFF191715)
private val border = Color(0xFF625B54)
private val accent = Color(0xFFF06543)
private val textMuted = Color(0xFFC3BBB2)
private val textPrimary = Color(0xFFF3EEE8)
private val readGreen = Color(0xFF67C587)
private val offlineBlue = Color(0xFF67AEE8)

fun main() {
    // Set the native appearance before AWT creates any windows.
    if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    }
    application {
        val rawHttp = remember { DesktopHttpTransport() }
        val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)
        Window(
            onCloseRequest = { rawHttp.close(); exitApplication() },
            title = "Manga Reader",
            state = windowState,
        ) {
            LaunchedEffect(Unit) { window.minimumSize = Dimension(980, 640) }
            val http = remember { RespectfulHttpTransport(rawHttp) }
            val store = remember { DesktopLibraryStore() }
            val repository = remember { MangaRepository(MangaInUaProvider(http), http, store) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = accent,
                onPrimary = Color(0xFF160D09),
                background = background,
                onBackground = textPrimary,
                surface = surface,
                onSurface = textPrimary,
                surfaceVariant = Color(0xFF302B26),
                onSurfaceVariant = textMuted,
                outline = border,
            )) {
                Surface(Modifier.fillMaxSize(), color = background, contentColor = textPrimary) {
                    Column(Modifier.fillMaxSize()) {
                        UpdateNotice(store.root)
                        ReaderApp(repository, store)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
    val settingsStore = remember(store.root) { DesktopSettingsStore(store.root) }
    var settings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }
    var settingsSaveError by remember { mutableStateOf<String?>(null) }
    val horizontal = settings.horizontal
    val pageWidth = settings.pageWidth
    val sidebarVisible = settings.sidebarVisible
    val hideToolbars = settings.hideToolbars
    val hideSidebar = settings.hideSidebar

    fun updateSettings(updated: ReaderSettings) {
        settings = updated.normalized()
        settingsSaveError = runCatching { settingsStore.save(settings) }.exceptionOrNull()?.let {
            "Could not save settings: ${it.message}"
        }
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var toolbarsRevealed by remember { mutableStateOf(false) }
    var revealedToolbarHeight by remember { mutableStateOf(0) }
    var showTools by remember { mutableStateOf(false) }
    var readChapters by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var offlineChapters by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedChapterIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionAnchorId by remember { mutableStateOf<String?>(null) }
    var shiftPressed by remember { mutableStateOf(false) }

    val chapterListState = rememberLazyListState()
    val readerListState = rememberLazyListState()
    val pageScrollStates = remember(chapter?.sourceId) { mutableMapOf<Int, ScrollState>() }
    // Measured from the reader itself in both modes. Deriving it from layoutInfo at
    // keypress time made the step depend on where the list happened to be.
    var readerViewportHeight by remember { mutableStateOf(0) }
    val pager = rememberPagerState(pageCount = { pages.size })
    val readerFocus = remember { FocusRequester() }
    val smallStep = with(LocalDensity.current) { 180.dp.toPx() }
    val autoHideActive = hideToolbars && !showGallery
    // Overlaid rather than laid out in the Row, so revealing it never resizes the
    // reader and never rescales the page.
    val autoHideSidebarActive = hideSidebar && !showGallery
    val upcomingChapter = nextChapterAfter(series?.chapters.orEmpty(), chapter?.sourceId)
    val atChapterEnd = pages.isNotEmpty() && if (horizontal) {
        val page = pageScrollStates[pager.currentPage]
        pager.currentPage == pages.lastIndex && (page == null || page.value >= page.maxValue)
    } else !readerListState.canScrollForward
    val revealEdge = with(LocalDensity.current) { 12.dp.toPx() }
    val hideBuffer = with(LocalDensity.current) { 16.dp.toPx() }

    var sidebarEdge by remember { mutableStateOf(0f) }

    fun updateToolbarHover(x: Float, y: Float) {
        if (!autoHideActive || showSettings || showAddDialog) return
        toolbarsRevealed = x >= sidebarEdge && if (toolbarsRevealed) y <= revealedToolbarHeight + hideBuffer else y <= revealEdge
    }

    LaunchedEffect(autoHideActive) { toolbarsRevealed = false }

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
        runCatching { repository.openChapter(selectedSeries.sourceId, selectedChapter.sourceId) }
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

    suspend fun openNextChapter() {
        val current = series ?: return
        val next = nextChapterAfter(current.chapters, chapter?.sourceId) ?: return
        showGallery = false
        selectChapter(current, next)
    }

    fun navigate(action: ReaderNavigationAction) {
        scope.launch {
            when (action) {
                ReaderNavigationAction.FirstPage -> if (pages.isNotEmpty()) {
                    if (horizontal) pager.animateScrollToPage(0) else readerListState.animateScrollToItem(0)
                }
                ReaderNavigationAction.LastPage -> if (pages.isNotEmpty()) {
                    if (horizontal) pager.animateScrollToPage(pages.lastIndex) else readerListState.animateScrollToItem(pages.lastIndex)
                }
                ReaderNavigationAction.SmallBackward -> readerListState.animateScrollBy(-smallStep)
                ReaderNavigationAction.SmallForward -> readerListState.animateScrollBy(smallStep)
                ReaderNavigationAction.ViewportBackward, ReaderNavigationAction.ViewportForward -> {
                    val forward = action == ReaderNavigationAction.ViewportForward
                    val direction = if (forward) 1 else -1
                    if (horizontal) {
                        val page = pageScrollStates[pager.currentPage]
                        val exhausted = page == null || page.value >= page.maxValue
                        if (forward && exhausted && pager.currentPage == pages.lastIndex) openNextChapter()
                        else page?.animateScrollBy(direction * readerViewportHeight.toFloat())
                    } else if (forward && !readerListState.canScrollForward) {
                        openNextChapter()
                    } else {
                        val height = readerViewportHeight.takeIf { it > 0 }
                            ?: readerListState.layoutInfo.run { viewportEndOffset - viewportStartOffset }
                        readerListState.animateScrollBy(direction * height.toFloat())
                    }
                }
                ReaderNavigationAction.PreviousPage -> if (pager.currentPage > 0) pager.animateScrollToPage(pager.currentPage - 1)
                ReaderNavigationAction.NextPage ->
                    if (pager.currentPage < pages.lastIndex) pager.animateScrollToPage(pager.currentPage + 1)
                    else openNextChapter()
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
    LaunchedEffect(showGallery, chapter?.sourceId, sidebarVisible, hideToolbars, hideSidebar, toolbarsRevealed, busy, showAddDialog, showSettings) {
        if (!showGallery && !busy && !showAddDialog && !showSettings && !toolbarsRevealed) readerFocus.requestFocus()
    }
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

    if (showSettings) AlertDialog(
        onDismissRequest = { showSettings = false },
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                settingsSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Reading", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show sidebar", Modifier.weight(1f))
                    Switch(sidebarVisible, { updateSettings(settings.copy(sidebarVisible = it)) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-hide top bars while reading", Modifier.weight(1f))
                    Switch(hideToolbars, { updateSettings(settings.copy(hideToolbars = it)) })
                }
                Text("Move to the top edge to reveal the bars. They hide when the pointer leaves the toolbar area.", color = textMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto-hide sidebar while reading", Modifier.weight(1f))
                    Switch(hideSidebar, { updateSettings(settings.copy(hideSidebar = it)) })
                }
                Text("The sidebar then opens over the page instead of shrinking it. Ctrl/Cmd+B still opens and closes it.", color = textMuted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Horizontal pages", Modifier.weight(1f))
                    Switch(horizontal, { updateSettings(settings.copy(horizontal = it)) })
                }
                Text("Page width · ${(pageWidth * 100).toInt()}%")
                Slider(pageWidth, { updateSettings(settings.copy(pageWidth = it)) }, valueRange = .25f..1f)
                Text("25% — 100% of the reading area", color = textMuted)
                Text("Keyboard controls", fontWeight = FontWeight.Bold)
                Text("Ctrl/Cmd+B: toggle sidebar\nCtrl/Cmd+T: reveal top bars while reading\nCtrl/Cmd+comma: settings\nEscape: reveal top bars\n\nPage Up / Down and Shift+Space / Space: scroll one screen.\nArrow keys: scroll vertically or switch images in horizontal mode.\nHome / End: first / last image.\n\nTab / Shift+Tab: move between controls\nEnter / Space: activate a focused button", color = textMuted)
            }
        },
        confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Done") } },
    )

    val sidebarPane: @Composable () -> Unit = {
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
            onOpenChapter = { item -> series?.let { active ->
                showGallery = false
                scope.launch { selectChapter(active, item) }
            } },
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
    }

    val appBar: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().background(surface).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sidebarVisible || !autoHideActive) IconButton(onClick = { updateSettings(settings.copy(sidebarVisible = !sidebarVisible)) }) {
                Icon(SidebarIcon, if (sidebarVisible) "Hide sidebar (Ctrl/Cmd+B)" else "Show sidebar (Ctrl/Cmd+B)", tint = if (sidebarVisible) accent else textMuted)
            } else Box(Modifier.width(48.dp).height(48.dp))
            Text("Manga Reader", Modifier.weight(1f), color = textMuted)
            if (!showGallery) IconButton(onClick = { updateSettings(settings.copy(hideToolbars = true)); toolbarsRevealed = false }) {
                Icon(ToolbarIcon, "Auto-hide top bars")
            }
            IconButton(onClick = { showSettings = true }) { Icon(SettingsIcon, "Settings (Ctrl/Cmd+comma)") }
        }
    }

    Box(
        Modifier.fillMaxSize().background(background)
            .onPointerEvent(PointerEventType.Move) { event -> event.changes.firstOrNull()?.let { updateToolbarHover(it.position.x, it.position.y) } }
            .onPointerEvent(PointerEventType.Enter) { event -> event.changes.firstOrNull()?.let { updateToolbarHover(it.position.x, it.position.y) } }
            .onPointerEvent(PointerEventType.Exit) { if (!showSettings && !showAddDialog) toolbarsRevealed = false }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) shiftPressed = event.type == KeyEventType.KeyDown
                if (showAddDialog || showSettings || event.type != KeyEventType.KeyDown || event.isAltPressed) {
                    false
                } else if (event.isCtrlPressed || event.isMetaPressed) {
                    when (event.key) {
                        Key.B -> { updateSettings(settings.copy(sidebarVisible = !sidebarVisible)); true }
                        Key.T -> if (autoHideActive) { toolbarsRevealed = true; true } else false
                        Key.Comma -> { showSettings = true; true }
                        else -> false
                    }
                } else if (event.key == Key.Escape && !showGallery && hideToolbars) {
                    toolbarsRevealed = true
                    true
                } else false
            }
    ) {
        Row(Modifier.fillMaxSize()) {
            // Pinned: occupies a Row slot, so the reader pane is narrower by design.
            androidx.compose.animation.AnimatedVisibility(
                visible = sidebarVisible && !autoHideSidebarActive,
                enter = expandHorizontally(tween(400, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
                    slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(400, easing = FastOutSlowInEasing)),
                exit = shrinkHorizontally(tween(400, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
                    slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(400, easing = FastOutSlowInEasing)),
            ) {
                sidebarPane()
            }

            Box(Modifier.weight(1f).fillMaxHeight().onGloballyPositioned { sidebarEdge = it.positionInRoot().x }) {
                Column(Modifier.fillMaxSize()) {
                    if (!autoHideActive) appBar()
                    if (showGallery) GalleryPane(library, readChapters, offlineChapters, repository, { scope.launch { openSeries(it) } }, { showAddDialog = true }, Modifier.weight(1f))
                    else Box(Modifier.weight(1f).fillMaxHeight()) {
                        ReaderPane(
                            repository, series, chapter, pages, horizontal, pageWidth, busy, readChapters, offlineChapters,
                            readerListState, pager, readerFocus,
                            pageScrollStates = pageScrollStates,
                            onViewportHeight = { readerViewportHeight = it },
                            onGallery = { showGallery = true },
                            onSetRead = { read -> series?.let { s -> chapter?.let { setRead(s.sourceId, setOf(it.sourceId), read) } } },
                            onMarkThrough = ::markThroughCurrent,
                            showToolbar = !hideToolbars,
                            onNavigate = ::navigate,
                            onCacheChanged = { syncMetadata() },
                            modifier = Modifier.fillMaxSize(),
                        )

                    }
                }
                if (autoHideActive) androidx.compose.animation.AnimatedVisibility(
                    visible = toolbarsRevealed,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(400, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(400, easing = FastOutSlowInEasing)),
                ) {
                    Column(Modifier.fillMaxWidth().onSizeChanged { revealedToolbarHeight = it.height }) {
                        appBar()
                        Box(Modifier.fillMaxWidth()) {
                            ReaderToolbar(
                                series, chapter, readChapters, offlineChapters,
                                onGallery = { showGallery = true },
                                onSetRead = { read -> series?.let { active -> chapter?.let { setRead(active.sourceId, setOf(it.sourceId), read) } } },
                                onMarkThrough = ::markThroughCurrent,
                            )
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = sidebarVisible && autoHideSidebarActive,
                    modifier = Modifier.align(Alignment.CenterStart),
                    enter = slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(400, easing = FastOutSlowInEasing)),
                    exit = slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(400, easing = FastOutSlowInEasing)),
                ) {
                    sidebarPane()
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !sidebarVisible && (autoHideActive || autoHideSidebarActive),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp),
                    enter = fadeIn(tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(400, easing = FastOutSlowInEasing)),
                ) {
                    IconButton(
                        onClick = { updateSettings(settings.copy(sidebarVisible = true)) },
                        modifier = Modifier.background(surface.copy(alpha = .9f), RoundedCornerShape(12.dp)),
                    ) {
                        Icon(SidebarIcon, "Show sidebar (Ctrl/Cmd+B)", tint = textMuted)
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showGallery && atChapterEnd && upcomingChapter != null,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    enter = slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(400, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(400, easing = FastOutSlowInEasing)),
                ) {
                    Row(
                        Modifier.background(surface.copy(alpha = .96f), RoundedCornerShape(14.dp))
                            .border(1.dp, border, RoundedCornerShape(14.dp))
                            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Chapter finished", color = textMuted, maxLines = 1)
                        Button(onClick = { scope.launch { openNextChapter() } }) {
                            Text(upcomingChapter?.label?.take(40) ?: "Next chapter", maxLines = 1)
                        }
                    }
                }
            }
        }
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
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text(if (downloading) "Pause download" else "Download", maxLines = 1) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && !downloading, onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
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
private fun ReaderToolbar(
    series: MangaSeries?, chapter: MangaChapter?, read: Map<String, Set<String>>, offline: Map<String, Set<String>>,
    onGallery: () -> Unit, onSetRead: (Boolean) -> Unit, onMarkThrough: () -> Unit,
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

        }
    }
}

@Composable
private fun ReaderPane(
    repository: MangaRepository, series: MangaSeries?, chapter: MangaChapter?, pages: List<MangaPage>,
    horizontal: Boolean, width: Float, busy: Boolean, read: Map<String, Set<String>>, offline: Map<String, Set<String>>,
    listState: androidx.compose.foundation.lazy.LazyListState, pager: androidx.compose.foundation.pager.PagerState,
    focus: FocusRequester, pageScrollStates: MutableMap<Int, ScrollState>, onViewportHeight: (Int) -> Unit,
    onGallery: () -> Unit, onSetRead: (Boolean) -> Unit, onMarkThrough: () -> Unit,
    showToolbar: Boolean, onNavigate: (ReaderNavigationAction) -> Unit,
    onCacheChanged: () -> Unit, modifier: Modifier,
) {
    Column(
        modifier.fillMaxHeight().focusRequester(focus).onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return@onKeyEvent false
            val action = readerNavigationAction(event.key, horizontal, event.isShiftPressed) ?: return@onKeyEvent false
            onNavigate(action); true
        }.focusable()
    ) {
        if (showToolbar) ReaderToolbar(series, chapter, read, offline, onGallery, onSetRead, onMarkThrough)
        if (pages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (busy) CircularProgressIndicator(color = accent) else Text("Select a chapter.", color = textMuted)
        } else if (horizontal) {
            LaunchedEffect(pager.currentPage, pages.size, chapter?.sourceId) {
                if (pager.currentPage == pages.lastIndex) onSetRead(true)
            }
            HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
                val scrollState = pageScrollStates.getOrPut(index) { ScrollState(0) }
                Box(
                    Modifier.fillMaxSize().onSizeChanged { onViewportHeight(it.height) }
                        .verticalScroll(scrollState).padding(10.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    PageImage(repository, series!!, chapter!!, pages[index], width, onCacheChanged)
                }
            }
        } else {
            LaunchedEffect(listState, pages.size, chapter?.sourceId) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == pages.lastIndex }
                    .distinctUntilChanged().collect { if (it) onSetRead(true) }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().onSizeChanged { onViewportHeight(it.height) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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

/**
 * Offers the newest release when one exists. Silent when the check is switched off,
 * when running unpackaged, or when GitHub cannot be reached - an update notice is
 * never worth an error in front of the reader.
 */
@Composable
private fun UpdateNotice(root: Path) {
    var release by remember { mutableStateOf<LatestRelease?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    LaunchedEffect(root) {
        val running = currentVersion() ?: return@LaunchedEffect
        if (!DesktopSettingsStore(root).load().checkForUpdates) return@LaunchedEffect
        val latest = fetchLatestRelease() ?: return@LaunchedEffect
        if (isNewer(latest.tag, running)) release = latest
    }
    val available = release
    if (available == null || dismissed) return
    Row(
        Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Version ${available.tag.removePrefix("v")} is available.",
            color = textPrimary,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { openInBrowser(available.url) }) { Text("Download") }
        TextButton(onClick = { dismissed = true }) { Text("Later") }
    }
}

private fun openInBrowser(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
