package com.gtavoapps.portylus.explorer

data class FolderRef(val uri: String, val name: String)
data class FolderLocation(val treeUri: String, val trail: List<FolderRef>) {
    init { require(trail.isNotEmpty()) }
    val current: FolderRef get() = trail.last()
    fun parent(): FolderLocation? = if (trail.size > 1) copy(trail = trail.dropLast(1)) else null
}
enum class SortOrder { NAME, MODIFIED }
data class DocumentEntry(val uri: String, val name: String, val isDirectory: Boolean, val modified: Long, val sizeBytes: Long? = null)
data class BrowserPreferences(
    val roots: List<FolderLocation> = emptyList(),
    val bookmarks: List<FolderLocation> = emptyList(),
    val lastLocation: FolderLocation? = null,
    val sort: SortOrder = SortOrder.NAME,
)

/** Forget a SAF grant and every shortcut that depends on it, without touching storage. */
fun BrowserPreferences.withoutRoot(treeUri: String) = copy(
    roots = roots.filterNot { it.treeUri == treeUri },
    bookmarks = bookmarks.filterNot { it.treeUri == treeUri },
    lastLocation = lastLocation?.takeUnless { it.treeUri == treeUri },
)

fun sortedEntries(entries: List<DocumentEntry>, order: SortOrder): List<DocumentEntry> =
    entries.sortedWith(compareBy<DocumentEntry> { !it.isDirectory }.thenComparator { a, b ->
        val comparison = if (order == SortOrder.MODIFIED) b.modified.compareTo(a.modified) else 0
        if (comparison != 0) comparison else a.name.compareTo(b.name, ignoreCase = true)
    })
