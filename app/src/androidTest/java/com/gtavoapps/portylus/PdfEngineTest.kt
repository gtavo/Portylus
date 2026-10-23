package com.gtavoapps.portylus

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.PDFAnnotation
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.PDFPage
import com.gtavoapps.portylus.annotations.*
import com.gtavoapps.portylus.pdf.PdfSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfEngineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val folder = File(instrumentation.targetContext.filesDir, "engine-test").apply { mkdirs() }
    private fun fixture(): File = File.createTempFile("base-", ".pdf", folder).also { file ->
        instrumentation.context.assets.open("base.pdf").use { input -> file.outputStream().use(input::copyTo) }
    }

    @Test fun nativeAnnotationsSurviveSaveReopenEditMoveDeleteAndUndo() {
        val source = fixture()
        val original = source.readBytes()
        val first = File(folder, "first.pdf").apply { delete() }
        val second = File(folder, "second.pdf").apply { delete() }
        val pen = Annotation(page = 0, kind = AnnotationKind.PEN,
            points = listOf(PagePoint(40f, 100f), PagePoint(80f, 120f), PagePoint(140f, 100f)))
        val highlight = Annotation(page = 0, kind = AnnotationKind.HIGHLIGHTER,
            points = listOf(PagePoint(40f, 55f), PagePoint(180f, 55f)), width = 18f, color = 0xffffff00.toInt())
        val text = Annotation(page = 1, kind = AnnotationKind.TEXT,
            box = PageBox(50f, 70f, 260f, 130f), text = "Nota: à é í ó ú ñ ç", textSize = 16f)
        PdfSession.open(source).use { session ->
            assertEquals(2, session.pageCount)
            assertEquals(300f, session.pageSize(0).width, 0.01f)
            session.add(pen); session.add(highlight); session.add(text)
            assertEquals(2, session.annotations(0).size)
            assertEquals(text.text, session.annotations(1).single().text)
            session.exportTo(first)
        }
        assertArrayEquals("Reading and export must never touch the original", original, source.readBytes())
        PdfSession.open(first).use { session ->
            val savedPen = session.annotations(0).first { it.id == pen.id }
            assertEquals(pen.points, savedPen.points)
            assertEquals(text.text, session.annotations(1).single().text)
            assertEquals(16f, session.annotations(1).single().textSize, 0.1f)
            val moved = PageBox(80f, 100f, 290f, 170f)
            session.editText(text.id, 1, "Texto editado después de reabrir", moved)
            assertEquals(moved, session.annotations(1).single().box)
            session.remove(pen.id, 0)
            assertEquals(1, session.annotations(0).size)
            assertTrue(session.undo())
            assertEquals(2, session.annotations(0).size)
            session.exportTo(second)
        }
        PdfSession.open(second).use { session ->
            assertEquals("Texto editado después de reabrir", session.annotations(1).single().text)
            assertEquals(2, session.annotations(0).size)
            val bitmap = session.render(0, 600)
            assertEquals(600, bitmap.width)
            assertTrue(bitmap.height > 600)
            bitmap.recycle()
        }
        verifyPreservation(source, second, highlight.id)
        verifyWithAndroidRenderer(second)
        // Available through run-as for independent inspection, never shipped as product content.
    }

    @Test fun foreignAnnotationsCannotBeDeletedAndSourceCannotBeOverwritten() {
        val source = fixture()
        PdfSession.open(source).use { session ->
            assertTrue(session.annotations(0).isEmpty())
            assertThrows(IllegalStateException::class.java) { session.remove("foreign-original", 0) }
            assertThrows(IllegalArgumentException::class.java) { session.exportTo(source) }
        }
    }

    private fun verifyPreservation(before: File, after: File, highlightId: String) {
        val base = Document.openDocument(before.path) as PDFDocument
        val saved = Document.openDocument(after.path) as PDFDocument
        try {
            assertEquals(base.countPages(), saved.countPages())
            for (index in 0 until base.countPages()) {
                val a = base.loadPage(index) as PDFPage
                val b = saved.loadPage(index) as PDFPage
                try {
                    assertEquals(a.`object`.get("Rotate").asInteger(), b.`object`.get("Rotate").asInteger())
                    assertArrayEquals(a.`object`.get("Contents").readStream(), b.`object`.get("Contents").readStream())
                    assertArrayEquals(a.`object`.get("MediaBox").toString().toByteArray(), b.`object`.get("MediaBox").toString().toByteArray())
                    assertEquals(a.`object`.get("CropBox").toString(), b.`object`.get("CropBox").toString())
                    assertArrayEquals(a.`object`.get("Resources").get("XObject").get("Im1").readStream(),
                        b.`object`.get("Resources").get("XObject").get("Im1").readStream())
                    assertTrue(b.search("Original").isNotEmpty())
                    val annotations = b.annotations ?: emptyArray()
                    try {
                        if (index == 0) {
                            assertTrue(annotations.any { it.`object`.get("NM").asString() == "foreign-original" })
                            val mark = annotations.single { it.`object`.get("NM").asString() == highlightId }
                            assertEquals(PDFAnnotation.TYPE_INK, mark.type)
                            assertEquals(0.28f, mark.opacity, 0.01f)
                            assertNotNull(mark.`object`.get("AP").get("N").readStream())
                        }
                    } finally { annotations.forEach { it.destroy() } }
                } finally { a.destroy(); b.destroy() }
            }
        } finally { base.destroy(); saved.destroy() }
    }

    private fun verifyWithAndroidRenderer(file: File) {
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(2, renderer.pageCount)
            for (index in 0 until renderer.pageCount) renderer.openPage(index).use { page ->
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                assertTrue(bitmap.width > 0)
                bitmap.recycle()
            }
        }
    }
}
