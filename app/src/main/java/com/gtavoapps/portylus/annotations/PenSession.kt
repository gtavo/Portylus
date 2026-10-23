package com.gtavoapps.portylus.annotations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Immutable vector states; one committed gesture is one undo step. No disk writes here. */
class PenSession(initial: List<Annotation> = emptyList()) {
    private var pages: Map<Int, List<Annotation>> = initial.groupBy { it.page }
    private val undoStates = ArrayDeque<Map<Int, List<Annotation>>>()
    private val redoStates = ArrayDeque<Map<Int, List<Annotation>>>()
    private var saved = snapshot()
    var hasStrokes by mutableStateOf(initial.isNotEmpty()); private set
    var isDirty by mutableStateOf(false); private set
    var canUndo by mutableStateOf(false); private set
    var canRedo by mutableStateOf(false); private set
    var revision by mutableIntStateOf(0); private set

    init { require(initial.all { it.kind != AnnotationKind.TEXT }) }
    fun strokes(page: Int): List<Annotation> = pages[page].orEmpty()
    fun snapshot(): List<Annotation> = pages.toSortedMap().values.flatten()

    fun add(stroke: Annotation) {
        require(stroke.kind != AnnotationKind.TEXT)
        commit(pages + (stroke.page to (strokes(stroke.page) + stroke)))
    }
    fun replace(page: Int, strokes: List<Annotation>) {
        require(strokes.all { it.page == page && it.kind != AnnotationKind.TEXT })
        commit(if (strokes.isEmpty()) pages - page else pages + (page to strokes.toList()))
    }
    private fun commit(next: Map<Int, List<Annotation>>) {
        if (next == pages) return
        undoStates.addLast(pages)
        if (undoStates.size > 50) undoStates.removeFirst()
        pages = next
        redoStates.clear()
        update()
    }
    fun undo() {
        if (undoStates.isEmpty()) return
        redoStates.addLast(pages)
        pages = undoStates.removeLast()
        update()
    }
    fun redo() {
        if (redoStates.isEmpty()) return
        undoStates.addLast(pages)
        pages = redoStates.removeLast()
        update()
    }
    fun markSaved(written: List<Annotation>) {
        saved = written.toList()
        update()
    }
    private fun update() {
        hasStrokes = pages.values.any { it.isNotEmpty() }
        isDirty = snapshot() != saved
        canUndo = undoStates.isNotEmpty()
        canRedo = redoStates.isNotEmpty()
        revision++
    }
}
