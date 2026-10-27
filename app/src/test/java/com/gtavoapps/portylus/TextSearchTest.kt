package com.gtavoapps.portylus

import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.*
import org.junit.Assert.*
import org.junit.Test

class TextSearchTest {
    private fun find(text: String, query: String, whole: Boolean = false, limit: Int = 5000): PageSearchResult {
        val glyphs = text.codePoints().toArray().mapIndexed { i, cp ->
            SearchGlyph(cp, PageQuad(PagePoint(i.toFloat(), 0f), PagePoint(i + 1f, 0f),
                PagePoint(i.toFloat(), 1f), PagePoint(i + 1f, 1f)))
        }
        return TextSearch.find(2, glyphs, query, whole, limit)
    }
    @Test fun partialMatchesInsideWordsAndWholeWordsRejectPrefixesSuffixesDigitsAndUnderscores() {
        assertEquals(6, find("PDF pdfizador PDF2 PDF_pdf pdf.", "pdf").matches.size)
        assertEquals(2, find("PDF pdfizador PDF2 PDF_pdf pdf.", "pdf", true).matches.size)
        assertEquals(1, find("pdfizador", "izador").matches.size)
        assertTrue(find("pdfizador", "izador", true).matches.isEmpty())
        assertEquals(2, find("(PDF),PDF-lector", "pdf", true).matches.size)
    }
    @Test fun unicodeBoundariesAndCaseAreNotLimitedToEnglish() {
        assertEquals(1, find("été ÉTÉment", "ÉTÉ", true).matches.size)
        assertEquals(1, find("ΚΌΣΜΟΣ κόσμος2", "κόσμος", true).matches.size)
        assertEquals(1, find("猫 猫咪", "猫", true).matches.size)
    }
    @Test fun composedAndDecomposedAccentsAndLigaturesAgree() {
        assertEquals(2, find("CAFÉ cafe\u0301", "café", true).matches.size)
        assertEquals(1, find("oﬃce", "OFFICE", true).matches.size)
        assertEquals(1, find("Straße", "STRASSE", true).matches.size)
    }
    @Test fun literalPunctuationCannotBecomeRegexAndWhitespaceCanCrossLines() {
        assertEquals(1, find("a.b aXb [x]", "a.b").matches.size)
        assertEquals(1, find("a.b aXb [x]", "[x]").matches.size)
        assertEquals(1, find("primera\n  línea", "PRIMERA línea", true).matches.size)
    }
    @Test fun geometryComesOnlyFromTheMatchedFragment() {
        val match = find("abcdef", "cd").matches.single()
        assertEquals(2, match.page)
        assertEquals(listOf(2f,3f), match.quads.map { it.upperLeft.x })
    }
    @Test fun limitsAreExplicitAndBlankQueriesNeverMatchEverything() {
        val result = find("a a a a", "a", limit = 2)
        assertEquals(2, result.matches.size); assertTrue(result.truncated)
        assertFalse(find("a a", "a", limit = 2).truncated)
        assertTrue(find("texto", "  ").matches.isEmpty())
        assertFalse(TextSearch.find(0, emptyList(), "algo", false).hasText)
    }
}
