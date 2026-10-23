package com.gtavoapps.portylus

import com.gtavoapps.portylus.annotations.*
import com.gtavoapps.portylus.annotations.Annotation
import org.junit.Assert.*
import org.junit.Test

class InkEraserTest {
    private fun stroke(kind: AnnotationKind = AnnotationKind.PEN, points: List<PagePoint> =
        listOf(PagePoint(0f, 0f), PagePoint(100f, 0f))) = Annotation(page = 0, kind = kind,
        points = points, width = 2f, color = 0xff123456.toInt())
    private fun erase(input: List<Annotation>, mode: EraserMode = EraserMode.PARTIAL,
        filter: AnnotationKind = AnnotationKind.PEN, a: PagePoint = PagePoint(50f, -40f),
        b: PagePoint = PagePoint(50f, 40f)) = InkEraser.erase(input, a, b, 4f, EraserOptions(mode, filter))

    @Test fun sparseFastCrossingSplitsExactlyAndPreservesStyle() {
        val original = stroke()
        val result = erase(listOf(original))
        assertEquals(2, result.size)
        assertEquals(45f, result[0].points.last().x, 0.001f)
        assertEquals(55f, result[1].points.first().x, 0.001f)
        assertEquals(0f, result[0].points.first().x, 0f)
        assertEquals(100f, result[1].points.last().x, 0f)
        assertTrue(result.all { it.color == original.color && it.width == original.width && it.page == 0 })
        assertEquals(2, result.map { it.id }.toSet().size)
        assertEquals(2, original.points.size)
    }

    @Test fun allFourCombinationsRespectFilter() {
        for (mode in EraserMode.entries) for (filter in listOf(AnnotationKind.PEN, AnnotationKind.HIGHLIGHTER)) {
            val pen = stroke(); val highlight = stroke(AnnotationKind.HIGHLIGHTER)
            val text = Annotation(page = 0, kind = AnnotationKind.TEXT, box = PageBox(0f, 0f, 100f, 20f), text = "Texto")
            val result = erase(listOf(pen, highlight, text), mode, filter)
            val protected = if (filter == AnnotationKind.PEN) highlight else pen
            assertTrue(result.any { it === protected })
            assertTrue(result.any { it === text })
            assertEquals(if (mode == EraserMode.PARTIAL) 2 else 0, result.count { it.kind == filter })
        }
    }

    @Test fun tapErasesDotAndLeavesDistantStrokeIdentical() {
        val dot = stroke(points = listOf(PagePoint(50f, 0f), PagePoint(50f, 0f)))
        val distant = stroke(points = listOf(PagePoint(0f, 80f), PagePoint(100f, 80f)))
        val result = erase(listOf(dot, distant), a = PagePoint(50f, 0f), b = PagePoint(50f, 0f))
        assertEquals(listOf(distant), result)
        assertSame(distant, result.single())
    }

    @Test fun endpointCapsAreIncludedAndNoBridgeRejoinsErasedParts() {
        val original = stroke(points = listOf(PagePoint(0f, 0f), PagePoint(50f, 0f), PagePoint(100f, 0f)))
        val result = erase(listOf(original), a = PagePoint(50f, 0f), b = PagePoint(50f, 0f))
        assertEquals(2, result.size)
        assertEquals(45f, result.first().points.last().x, 0.001f)
        assertEquals(55f, result.last().points.first().x, 0.001f)
    }

    @Test fun collinearSweepCanRemoveEntireLine() {
        assertTrue(erase(listOf(stroke()), a = PagePoint(-10f, 0f), b = PagePoint(110f, 0f)).isEmpty())
    }

    @Test fun curvedStrokeCanProduceThreeSurvivors() {
        val original = stroke(points = listOf(PagePoint(0f, 0f), PagePoint(100f, 0f),
            PagePoint(100f, 30f), PagePoint(0f, 30f)))
        val result = erase(listOf(original))
        assertEquals(3, result.size)
        assertTrue(result.all { it.points.size >= 2 })
    }
}
