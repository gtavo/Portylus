package com.gtavoapps.portylus.pdf

import android.graphics.Bitmap
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.artifex.mupdf.fitz.Point
import com.artifex.mupdf.fitz.Rect
import com.artifex.mupdf.fitz.StructuredText
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PageBox
import com.gtavoapps.portylus.annotations.PagePoint
import java.io.File

data class PageSize(val width: Float, val height: Float, val originX: Float = 0f, val originY: Float = 0f)

/** All JNI access is serialized, including close vs cancelled rendering work. */
class PdfSession private constructor(private val document: PDFDocument, private val source: File) : AutoCloseable {
    private var closed = false
    // At most one extracted page; drawing never invokes JNI text extraction on the UI thread.
    private var textPage = -1
    private var structuredText: StructuredText? = null
    private var textHitBoxes = emptyList<Rect>()
    val pageCount: Int = document.countPages()
    val canAnnotate: Boolean = document.hasPermission(Document.PERMISSION_ANNOTATE)

    /** Rebuild only Portylus ink. Foreign annotations and native text notes remain intact. */
    @Synchronized fun replaceInk(strokes: List<Annotation>) {
        require(canAnnotate) { "El PDF no permite anotaciones." }
        require(strokes.all { it.kind != AnnotationKind.TEXT })
        for (page in 0 until pageCount) {
            annotations(page).filter { it.kind != AnnotationKind.TEXT }.forEach { remove(it.id, page) }
        }
        strokes.forEach(::add)
    }


    @Synchronized fun selectText(index: Int, start: PagePoint, end: PagePoint, tolerance: Float): TextSelection {
        check(!closed)
        require(index in 0 until pageCount)
        require(listOf(start.x, start.y, end.x, end.y, tolerance).all { it.isFinite() } && tolerance >= 0f)
        require(document.hasPermission(Document.PERMISSION_COPY)) { "El PDF no permite copiar texto." }
        if (textPage != index) {
            clearTextCache()
            val extracted = withPage(index) { it.toStructuredText() }
            try {
                textHitBoxes = extracted.blocks.flatMap { it.lines.toList() }
                    .flatMap { it.chars.toList() }.filterNot { it.isWhitespace }.map { it.quad.toRect() }
                structuredText = extracted
                textPage = index
            } catch (error: Throwable) { extracted.destroy(); throw error }
        }
        val empty = TextSelection(index, "", emptyList())
        if (textHitBoxes.none { start.x >= it.x0 - tolerance && start.x <= it.x1 + tolerance &&
                start.y >= it.y0 - tolerance && start.y <= it.y1 + tolerance }) return empty
        val text = requireNotNull(structuredText)
        val a = Point(start.x, start.y)
        val b = Point(end.x, end.y)
        text.snapSelection(a, b, StructuredText.SELECT_WORDS)
        val copied = text.copy(a, b).orEmpty().trim()
        if (copied.isEmpty()) return empty
        fun Quad.coordinates() = PageQuad(PagePoint(ul_x, ul_y), PagePoint(ur_x, ur_y),
            PagePoint(ll_x, ll_y), PagePoint(lr_x, lr_y))
        return TextSelection(index, copied, text.highlight(a, b).orEmpty().map { it.coordinates() })
    }

    @Synchronized fun searchText(index: Int, query: String, wholeWords: Boolean,
        limit: Int = 5000): PageSearchResult = withPage(index) { page ->
        val text = page.toStructuredText()
        try {
            val glyphs = ArrayList<SearchGlyph>()
            for (block in text.blocks) {
                for (line in block.lines) {
                    for (char in line.chars) {
                        val q = char.quad
                        glyphs.add(SearchGlyph(char.c, PageQuad(PagePoint(q.ul_x, q.ul_y),
                            PagePoint(q.ur_x, q.ur_y), PagePoint(q.ll_x, q.ll_y), PagePoint(q.lr_x, q.lr_y))))
                    }
                    glyphs.add(SearchGlyph(' '.code, null))
                }
            }
            TextSearch.find(index, glyphs, query, wholeWords, limit)
        } finally { text.destroy() }
    }

    private fun clearTextCache() {
        structuredText?.destroy()
        structuredText = null
        textHitBoxes = emptyList()
        textPage = -1
    }

    @Synchronized fun pageSize(index: Int): PageSize = withPage(index) { page ->
        val b = page.bounds
        require(b.x1 > b.x0 && b.y1 > b.y0 && listOf(b.x0, b.x1, b.y0, b.y1).all { it.isFinite() }) {
            "La página tiene unas dimensiones no válidas."
        }
        PageSize(b.x1 - b.x0, b.y1 - b.y0, b.x0, b.y0)
    }

    @Synchronized fun render(index: Int, width: Int): Bitmap = withPage(index) { page ->
        val size = page.bounds
        require(size.x1 > size.x0 && size.y1 > size.y0 &&
            listOf(size.x0, size.x1, size.y0, size.y1).all { it.isFinite() }) {
            "La página tiene unas dimensiones no válidas."
        }
        val scale = width.coerceIn(1, 2400) / (size.x1 - size.x0)
        // Bound both dimensions: an unusually long page must not allocate unbounded memory.
        val pixels = (size.x1 - size.x0) * (size.y1 - size.y0)
        val safeScale = minOf(scale, 4096f / (size.y1 - size.y0),
            kotlin.math.sqrt((3.5f * 1024 * 1024) / pixels))
        AndroidDrawDevice.drawPage(page, Matrix(safeScale))
    }

    @Synchronized fun annotations(index: Int): List<Annotation> = withPage(index) { page ->
        val native = page.annotations ?: emptyArray()
        try { native.mapNotNull { readOwnAnnotation(it, index) } }
        finally { native.forEach { it.destroy() } }
    }

    @Synchronized fun add(annotation: Annotation) {
        require(document.hasPermission(Document.PERMISSION_ANNOTATE)) { "El PDF no permite anotaciones." }
        operation("Añadir anotación") {
            withPage(annotation.page) { page ->
                val native = page.createAnnotation(if (annotation.kind == AnnotationKind.TEXT)
                    PDFAnnotation.TYPE_FREE_TEXT else PDFAnnotation.TYPE_INK)
                try {
                    native.`object`.put(OWNER_KEY, OWNER_VALUE)
                    native.`object`.put("NM", annotation.id)
                    native.`object`.put(KIND_KEY, annotation.kind.name)
                    val rgb = floatArrayOf(((annotation.color shr 16) and 255) / 255f,
                        ((annotation.color shr 8) and 255) / 255f, (annotation.color and 255) / 255f)
                    native.flags = native.flags or PDFAnnotation.IS_PRINT
                    if (annotation.kind == AnnotationKind.TEXT) {
                        val b = requireNotNull(annotation.box)
                        native.rect = Rect(b.left, b.top, b.right, b.bottom)
                        native.contents = annotation.text
                        native.setDefaultAppearance("Helv", annotation.textSize, rgb)
                    } else {
                        native.inkList = arrayOf(annotation.points.map { Point(it.x, it.y) }.toTypedArray())
                        native.borderWidth = annotation.width
                        native.color = rgb
                        native.opacity = if (annotation.kind == AnnotationKind.HIGHLIGHTER) 72f / 255f else 1f
                        if (annotation.kind == AnnotationKind.HIGHLIGHTER) {
                            val blend = document.newName("Multiply")
                            try { native.`object`.put("BM", blend) } finally { blend.destroy() }
                        }
                    }
                    native.update()
                    page.update()
                } finally { native.destroy() }
            }
        }
    }

    @Synchronized fun editText(id: String, index: Int, text: String, box: PageBox) {
        require(text.isNotBlank())
        require(listOf(box.left, box.top, box.right, box.bottom).all { it.isFinite() })
        require(box.right > box.left && box.bottom > box.top)
        operation("Editar texto") {
            withPage(index) { page ->
                visitOwn(page, id) { native ->
                    require(native.type == PDFAnnotation.TYPE_FREE_TEXT)
                    native.rect = Rect(box.left, box.top, box.right, box.bottom)
                    native.contents = text
                    native.update()
                    page.update()
                }
            }
        }
    }

    @Synchronized fun remove(id: String, index: Int) {
        operation("Borrar anotación") {
            withPage(index) { page -> visitOwn(page, id) { page.deleteAnnotation(it); page.update() } }
        }
    }

    @Synchronized fun undo(): Boolean {
        check(!closed)
        if (!document.canUndo()) return false
        document.undo()
        clearTextCache()
        return true
    }

    /** Prepare and reopen a distinct private file before the verified SAF write. */
    @Synchronized fun exportTo(destination: File) {
        check(!closed)
        require(destination.canonicalFile != source.canonicalFile) { "Se requiere un destino temporal distinto." }
        require(!destination.exists()) { "El destino ya existe." }
        document.save(destination.absolutePath, "")
        check(destination.length() > 0L)
        val verified = open(destination)
        try { check(verified.pageCount == pageCount) }
        finally { verified.close() }
    }

    @Synchronized override fun close() {
        if (!closed) { closed = true; clearTextCache(); document.destroy() }
    }

    private fun <T> withPage(index: Int, block: (PDFPage) -> T): T {
        check(!closed)
        require(index in 0 until pageCount)
        val page = document.loadPage(index) as PDFPage
        try { return block(page) } finally { page.destroy() }
    }

    private fun operation(name: String, block: () -> Unit) {
        check(!closed)
        document.beginOperation(name)
        try { block(); document.endOperation(); clearTextCache() }
        catch (error: Throwable) { document.abandonOperation(); throw error }
    }

    private fun visitOwn(page: PDFPage, id: String, block: (PDFAnnotation) -> Unit) {
        val native = page.annotations ?: emptyArray()
        try {
            val annotation = native.firstOrNull {
                it.`object`.get(OWNER_KEY).asString() == OWNER_VALUE &&
                    it.`object`.get("NM").asString() == id
            } ?: error("No se encuentra una anotación propia con ese identificador.")
            block(annotation)
        } finally { native.forEach { it.destroy() } }
    }

    private fun readOwnAnnotation(native: PDFAnnotation, index: Int): Annotation? {
        val dictionary = native.`object`
        if (dictionary.get(OWNER_KEY).asString() != OWNER_VALUE) return null
        val kind = runCatching { AnnotationKind.valueOf(dictionary.get(KIND_KEY).asString()) }.getOrNull()
            ?: return null
        val id = dictionary.get("NM").asString()
        if (id.isNullOrBlank()) return null
        return if (kind == AnnotationKind.TEXT && native.type == PDFAnnotation.TYPE_FREE_TEXT) {
            val b = native.rect
            val appearance = native.defaultAppearance
            Annotation(id, index, kind, box = PageBox(b.x0, b.y0, b.x1, b.y1),
                text = native.contents, textSize = appearance.size, color = packColor(appearance.color))
        } else if (kind != AnnotationKind.TEXT && native.type == PDFAnnotation.TYPE_INK) {
            val points = native.inkList?.firstOrNull()?.map { PagePoint(it.x, it.y) }.orEmpty()
            if (points.size < 2) null else Annotation(id, index, kind, points = points,
                width = native.borderWidth, color = packColor(native.color))
        } else null
    }

    private fun packColor(rgb: FloatArray): Int {
        if (rgb.size < 3) return 0xff000000.toInt()
        fun channel(i: Int) = (rgb[i].coerceIn(0f, 1f) * 255 + 0.5f).toInt()
        return 0xff000000.toInt() or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    }

    companion object {
        private const val OWNER_KEY = "GtavoPdfOwner"
        // Persisted PDF protocol: retain compatibility with annotations saved by earlier versions.
        private const val OWNER_VALUE = "es.gtavo.pdf/v1"
        private const val KIND_KEY = "GtavoPdfKind"

        fun open(file: File): PdfSession {
            val raw = Document.openDocument(file.absolutePath)
            try {
                require(!raw.needsPassword()) { "Este PDF necesita contraseña; todavía no se admite." }
                require(raw is PDFDocument) { "El archivo no es un PDF." }
                require(raw.countPages() > 0) { "El PDF no contiene páginas." }
                raw.disableJs()
                raw.enableJournal()
                return PdfSession(raw, file)
            } catch (error: Throwable) { raw.destroy(); throw error }
        }
    }
}
