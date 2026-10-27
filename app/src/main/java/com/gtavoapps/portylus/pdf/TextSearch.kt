package com.gtavoapps.portylus.pdf

import java.text.Normalizer
import java.util.Locale

data class SearchGlyph(val codePoint: Int, val quad: PageQuad?)
data class SearchMatch(val page: Int, val quads: List<PageQuad>)
data class PageSearchResult(val matches: List<SearchMatch>, val hasText: Boolean, val truncated: Boolean)

/** Literal Unicode search. Geometry stays in PDF coordinates; nothing is written to the PDF. */
object TextSearch {
    private fun fold(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKD).uppercase(Locale.ROOT).lowercase(Locale.ROOT).replace('\u03c2', '\u03c3')
    private fun space(c: Int) = Character.isWhitespace(c) || Character.isSpaceChar(c)
    private fun mark(c: Int) = Character.getType(c) in listOf(Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt())
    private fun word(c: Int) = Character.isLetterOrDigit(c) || mark(c) || c == '_'.code

    fun find(page: Int, glyphs: List<SearchGlyph>, query: String, wholeWords: Boolean,
        limit: Int = 5000): PageSearchResult {
        require(limit >= 0)
        val needle = fold(query).replace(Regex("[\\p{Z}\\s]+"), " ").trim()
        val hasText = glyphs.any { !space(it.codePoint) }
        if (needle.isEmpty()) return PageSearchResult(emptyList(), hasText, false)
        val text = StringBuilder()
        val owners = ArrayList<List<PageQuad>>()
        // Keep combining marks with their base character so composed/decomposed accents agree.
        var index = 0
        while (index < glyphs.size) {
            val cluster = ArrayList<SearchGlyph>()
            cluster.add(glyphs[index++])
            while (index < glyphs.size && mark(glyphs[index].codePoint)) cluster.add(glyphs[index++])
            val raw = buildString { cluster.forEach { appendCodePoint(it.codePoint) } }
            val boxes = cluster.mapNotNull { it.quad }.distinct()
            for (c in fold(raw)) {
                if (space(c.code)) {
                    if (text.isNotEmpty() && text.last() != ' ') { text.append(' '); owners.add(emptyList()) }
                } else { text.append(c); owners.add(boxes) }
            }
        }
        val haystack = text.toString()
        val hits = ArrayList<SearchMatch>()
        var from = 0
        while (from <= haystack.length - needle.length) {
            val start = haystack.indexOf(needle, from)
            if (start < 0) break
            val end = start + needle.length
            val bounded = !wholeWords || ((start == 0 || !word(haystack.codePointBefore(start))) &&
                (end == haystack.length || !word(haystack.codePointAt(end))))
            if (bounded) {
                val boxes = owners.subList(start, end).flatten().distinct()
                if (boxes.isNotEmpty()) {
                    if (hits.size == limit) return PageSearchResult(hits, hasText, true)
                    hits.add(SearchMatch(page, boxes))
                }
            }
            from = end
        }
        return PageSearchResult(hits, hasText, false)
    }
}
