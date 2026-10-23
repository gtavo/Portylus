package com.gtavoapps.portylus

import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.PagePlacement
import com.gtavoapps.portylus.pdf.PageSize
import org.junit.Assert.*
import org.junit.Test

class PagePlacementTest {
    @Test fun roundTripPreservesPageCoordinatesAcrossZoomScrollAndCropOrigins() {
        val size = PageSize(280f, 380f, 10f, -20f)
        for (scale in listOf(0.5f, 1f, 2f, 4f)) {
            val page = PagePlacement(3, -147f, -350f, scale, size)
            for (point in listOf(PagePoint(10f, -20f), PagePoint(90f, 130f), PagePoint(290f, 360f))) {
                val screen = page.toScreen(point)
                val recovered = requireNotNull(page.toPage(screen.x, screen.y))
                assertEquals(point.x, recovered.x, 0.001f)
                assertEquals(point.y, recovered.y, 0.001f)
            }
        }
    }
    @Test fun resizedViewportDoesNotChangeTheStoredStrokePosition() {
        val point = PagePoint(50f, 80f)
        val before = PagePlacement(0, 0f, 12f, 2f, PageSize(300f, 400f))
        val after = PagePlacement(0, -40f, -120f, 3f, PageSize(300f, 400f))
        assertNotEquals(before.toScreen(point), after.toScreen(point))
        assertEquals(point, after.toPage(after.toScreen(point).x, after.toScreen(point).y))
    }
    @Test fun gapsAndNonFiniteCoordinatesNeverBecomePageHits() {
        val page = PagePlacement(1, 0f, 20f, 2f, PageSize(300f, 400f))
        for (point in listOf(PagePoint(-1f, 30f), PagePoint(100f, 19f), PagePoint(601f, 40f),
            PagePoint(100f, 821f), PagePoint(Float.NaN, 100f), PagePoint(100f, Float.POSITIVE_INFINITY))) {
            assertNull(page.toPage(point.x, point.y))
        }
    }
}
