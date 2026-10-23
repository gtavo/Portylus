package com.gtavoapps.portylus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artifex.mupdf.fitz.Document
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.PdfSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TextSelectionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    /** Two text lines; the second page has both cropping and rotation. */
    private fun fixture(text: Boolean = true): File {
        val content = if (text) "BT /F1 14 Tf 30 240 Td (Primera l\\355nea con acentos.) Tj " +
            "0 -25 Td (Segunda frase seleccionable.) Tj ET\n" else "0.8 g 30 200 200 70 re f\n"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R 6 0 R] /Count 2 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
            "<< /Length ${content.length} >>\nstream\n${content}endstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /CropBox [10 10 290 390] /Rotate 90 " +
                "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>")
        val pdf = StringBuilder("%PDF-1.7\n")
        val offsets = objects.mapIndexed { i, obj -> pdf.length.also { pdf.append("${i + 1} 0 obj\n$obj\nendobj\n") } }
        val xref = pdf.length
        pdf.append("xref\n0 7\n0000000000 65535 f \n")
        offsets.forEach { pdf.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        pdf.append("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return File.createTempFile("selection-", ".pdf", instrumentation.targetContext.filesDir).apply {
            writeBytes(pdf.toString().toByteArray(Charsets.US_ASCII))
        }
    }

    @Test fun nativeSelectionSupportsWordsMultilineReverseAccentsAndRotatedCroppedPages() {
        val source = fixture()
        val original = source.readBytes()
        try {
            PdfSession.open(source).use { session ->
                val native = Document.openDocument(source.absolutePath)
                try {
                    for (index in 0..1) {
                        val page = native.loadPage(index)
                        val text = page.toStructuredText()
                        try {
                            val lines = text.blocks.flatMap { it.lines.toList() }
                            fun center(line: Int, char: Int): PagePoint {
                                val q = lines[line].chars[char].quad
                                return PagePoint((q.ul_x + q.lr_x) / 2, (q.ul_y + q.lr_y) / 2)
                            }
                            val a = center(0, 0)
                            val b = center(1, lines[1].chars.lastIndex)
                            val selection = session.selectText(index, a, b, 0f)
                            assertEquals("Primera línea con acentos.\nSegunda frase seleccionable.", selection.text)
                            assertTrue(selection.quads.size >= 2)
                            assertEquals(selection.text, session.selectText(index, b, a, 0f).text)
                            assertEquals("Primera", session.selectText(index, a, a, 0f).text)
                            assertEquals("", session.selectText(index, PagePoint(1f, 1f), b, 0f).text)
                            val size = session.pageSize(index)
                            for (q in selection.quads) for (point in listOf(q.upperLeft, q.upperRight, q.lowerLeft, q.lowerRight)) {
                                assertTrue(point.x in size.originX..(size.originX + size.width))
                                assertTrue(point.y in size.originY..(size.originY + size.height))
                            }
                        } finally { text.destroy(); page.destroy() }
                    }
                } finally { native.destroy() }
            }
            assertArrayEquals(original, source.readBytes())
        } finally { source.delete() }
    }

    @Test fun pagesWithoutATextLayerProduceNoSelection() {
        val source = fixture(text = false)
        try {
            PdfSession.open(source).use { session ->
                val result = session.selectText(0, PagePoint(35f, 150f), PagePoint(200f, 150f), 10f)
                assertTrue(result.text.isEmpty())
                assertTrue(result.quads.isEmpty())
            }
        } finally { source.delete() }
    }
}
