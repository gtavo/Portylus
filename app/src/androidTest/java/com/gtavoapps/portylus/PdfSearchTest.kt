package com.gtavoapps.portylus

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.pdf.PdfSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfSearchTest {
    @Test fun nativeTextSearchSupportsPartialWholeWordsAccentsAndRotatedCroppedGeometryWithoutSaving() {
        val source = SearchFixture.create(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = source.readBytes()
        try {
            PdfSession.open(source).use { pdf ->
                for (page in 0..1) {
                    assertEquals(6, pdf.searchText(page,"pdf",false).matches.size)
                    assertEquals(2, pdf.searchText(page,"PDF",true).matches.size)
                    assertEquals(1, pdf.searchText(page,"izador",false).matches.size)
                    assertTrue(pdf.searchText(page,"izador",true).matches.isEmpty())
                    assertEquals(2, pdf.searchText(page,"café",true).matches.size)
                    assertEquals(1, pdf.searchText(page,"Primera línea",true).matches.size)
                    val result = pdf.searchText(page,"[x]",false)
                    assertEquals(1, result.matches.size)
                    val size = pdf.pageSize(page)
                    for (quad in result.matches.single().quads) {
                        for (point in listOf(quad.upperLeft,quad.upperRight,quad.lowerLeft,quad.lowerRight)) {
                            assertTrue(point.x in size.originX..size.originX+size.width)
                            assertTrue(point.y in size.originY..size.originY+size.height)
                        }
                    }
                    assertTrue(pdf.annotations(page).isEmpty())
                }
                val blank = pdf.searchText(2,"pdf",false)
                assertFalse(blank.hasText); assertTrue(blank.matches.isEmpty())
            }
            assertArrayEquals(original,source.readBytes())
        } finally { source.delete() }
    }
}
