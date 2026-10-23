package com.gtavoapps.portylus.pdf

import com.gtavoapps.portylus.annotations.PagePoint

/** PDF coordinates, including the page's crop/rotation transform. No native objects escape. */
data class PageQuad(val upperLeft: PagePoint, val upperRight: PagePoint,
    val lowerLeft: PagePoint, val lowerRight: PagePoint)

data class TextSelection(val page: Int, val text: String, val quads: List<PageQuad>)
