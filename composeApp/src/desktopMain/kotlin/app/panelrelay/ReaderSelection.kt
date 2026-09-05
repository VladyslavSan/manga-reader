package app.panelrelay

internal data class ChapterSelectionUpdate(val selectedIds: Set<String>, val anchorId: String?)

internal fun updateChapterSelection(
    orderedChapterIds: List<String>,
    selectedIds: Set<String>,
    anchorId: String?,
    clickedId: String,
    selected: Boolean,
    extendRange: Boolean,
): ChapterSelectionUpdate {
    val clickedIndex = orderedChapterIds.indexOf(clickedId)
    if (clickedIndex < 0) return ChapterSelectionUpdate(selectedIds, anchorId)
    val anchorIndex = if (extendRange) orderedChapterIds.indexOf(anchorId) else -1
    val affected = if (anchorIndex >= 0) {
        (minOf(anchorIndex, clickedIndex)..maxOf(anchorIndex, clickedIndex)).mapTo(mutableSetOf()) { orderedChapterIds[it] }
    } else setOf(clickedId)
    return ChapterSelectionUpdate(
        if (selected) selectedIds + affected else selectedIds - affected,
        if (anchorIndex >= 0) anchorId else clickedId,
    )
}

