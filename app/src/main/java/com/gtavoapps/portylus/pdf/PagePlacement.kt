package com.gtavoapps.portylus.pdf

import com.gtavoapps.portylus.annotations.PagePoint

/** The same transform is used for the bitmap, pen preview and committed vector strokes. */
data class PagePlacement(
    val page: Int,
    val left: Float,
    val top: Float,
    val scale: Float,
    val size: PageSize,
) {
    val right: Float get() = left + size.width * scale
    val bottom: Float get() = top + size.height * scale

    fun toPage(x: Float, y: Float): PagePoint? {
        if (!x.isFinite() || !y.isFinite() || x < left || x > right || y < top || y > bottom) return null
        return PagePoint((x - left) / scale + size.originX, (y - top) / scale + size.originY)
    }

    fun toScreen(point: PagePoint) = PagePoint(
        left + (point.x - size.originX) * scale,
        top + (point.y - size.originY) * scale,
    )
}
