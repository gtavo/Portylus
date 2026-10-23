package com.gtavoapps.portylus.annotations

import java.util.UUID

data class PagePoint(val x: Float, val y: Float)
data class PageBox(val left: Float, val top: Float, val right: Float, val bottom: Float)
enum class AnnotationKind { PEN, HIGHLIGHTER, TEXT }

data class Annotation(
    val id: String = UUID.randomUUID().toString(),
    val page: Int,
    val kind: AnnotationKind,
    val points: List<PagePoint> = emptyList(),
    val box: PageBox? = null,
    val color: Int = 0xff2255cc.toInt(),
    val width: Float = 2f,
    val text: String = "",
    val textSize: Float = 14f,
) {
    init {
        require(page >= 0)
        require(width.isFinite() && width > 0f)
        require(textSize.isFinite() && textSize > 0f)
        require(points.all { it.x.isFinite() && it.y.isFinite() })
        if (kind == AnnotationKind.TEXT) {
            require(box != null && text.isNotBlank())
            require(listOf(box.left, box.top, box.right, box.bottom).all { it.isFinite() })
            require(box.right > box.left && box.bottom > box.top)
        } else require(points.size >= 2)
    }
}
