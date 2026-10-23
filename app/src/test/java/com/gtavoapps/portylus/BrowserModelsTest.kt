package com.gtavoapps.portylus

import com.gtavoapps.portylus.explorer.*
import org.junit.Assert.*
import org.junit.Test

class BrowserModelsTest {
    @Test fun forgettingRootRemovesDependentShortcutsAndLastFolderOnly() {
        val root = FolderLocation("tree", listOf(FolderRef("root", "Root")))
        val child = root.copy(trail = root.trail + FolderRef("child", "Child"))
        val other = FolderLocation("other", listOf(FolderRef("other-root", "Other")))
        val original = BrowserPreferences(listOf(root, other), listOf(child, other), child)
        val result = original.withoutRoot("tree")
        assertEquals(listOf(other), result.roots)
        assertEquals(listOf(other), result.bookmarks)
        assertNull(result.lastLocation)
        assertEquals(original, original.withoutRoot("absent"))
    }
    private val entries = listOf(DocumentEntry("b", "b.PDF", false, 12),
        DocumentEntry("d", "zeta", true, 2), DocumentEntry("a", "A.pdf", false, 4))
    @Test fun foldersStayFirstAndNamesIgnoreCase() {
        assertEquals(listOf("d", "a", "b"), sortedEntries(entries, SortOrder.NAME).map { it.uri })
    }
    @Test fun modifiedSortKeepsFoldersFirstThenNewestPdf() {
        assertEquals(listOf("d", "b", "a"), sortedEntries(entries, SortOrder.MODIFIED).map { it.uri })
    }
    @Test fun parentNeverEscapesTheAuthorizedRoot() {
        val root = FolderLocation("tree", listOf(FolderRef("root", "Root")))
        assertNull(root.parent())
        assertEquals(root, root.copy(trail = root.trail + FolderRef("child", "Child")).parent())
    }
}
