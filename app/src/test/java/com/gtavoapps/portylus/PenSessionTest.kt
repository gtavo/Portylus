package com.gtavoapps.portylus
import com.gtavoapps.portylus.annotations.*
import com.gtavoapps.portylus.annotations.Annotation
import org.junit.Assert.*
import org.junit.Test
class PenSessionTest {
    private fun stroke(page: Int = 0) = Annotation(page = page, kind = AnnotationKind.PEN,
        points = listOf(PagePoint(1f, 2f), PagePoint(3f, 4f)))
    @Test fun undoRedoAndBranchTrackTheExplicitSavedCheckpoint() {
        val pen = PenSession()
        val a = stroke(); val b = stroke()
        pen.add(a); assertTrue(pen.isDirty); assertTrue(pen.canUndo)
        pen.markSaved(pen.snapshot()); assertFalse(pen.isDirty)
        pen.add(b); pen.undo(); assertEquals(listOf(a), pen.snapshot()); assertFalse(pen.isDirty)
        pen.undo(); assertTrue(pen.isDirty); assertFalse(pen.hasStrokes)
        pen.redo(); assertFalse(pen.isDirty); pen.redo(); assertTrue(pen.isDirty)
        pen.undo(); pen.add(stroke(1)); assertFalse(pen.canRedo)
    }
    @Test fun loadedInkStartsCleanAndEraseIsOneUndoStep() {
        val a=stroke(); val b=stroke(1); val pen=PenSession(listOf(a,b))
        assertFalse(pen.isDirty); assertFalse(pen.canUndo); assertTrue(pen.hasStrokes)
        pen.replace(0, emptyList()); assertTrue(pen.isDirty)
        pen.undo(); assertEquals(listOf(a,b),pen.snapshot()); assertFalse(pen.isDirty)
        pen.redo(); assertEquals(listOf(b),pen.snapshot())
        pen.markSaved(pen.snapshot()); pen.undo(); assertTrue(pen.isDirty)
    }
    @Test fun noOpEraseDoesNotChangeHistoryAndHistoryIsBounded() {
        val pen=PenSession()
        pen.replace(0,emptyList()); assertFalse(pen.canUndo); assertFalse(pen.isDirty)
        repeat(70) { pen.add(stroke()) }
        repeat(80) { pen.undo() }
        assertEquals(20,pen.snapshot().size); assertFalse(pen.canUndo)
        repeat(80) { pen.redo() }; assertEquals(70,pen.snapshot().size); assertFalse(pen.canRedo)
    }
}
