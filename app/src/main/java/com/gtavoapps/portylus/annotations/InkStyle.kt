package com.gtavoapps.portylus.annotations

/** Opaque RGB and PDF page units; opacity belongs to the tool, never to a preset. */
data class InkStyle(val color: Int, val width: Float) {
    init {
        require(color ushr 24 == 255)
        require(width.isFinite() && width in 0.5f..32f)
    }
}

data class ToolPresets(val slots: List<InkStyle>, val selected: Int = 0) {
    init { require(slots.size == 3 && selected in 0..2) }
    val current: InkStyle get() = slots[selected]
    fun save(index: Int, style: InkStyle): ToolPresets {
        require(index in 0..2)
        return ToolPresets(slots.mapIndexed { i, old -> if (i == index) style else old }, index)
    }
}

data class InkPreferences(
    val pen: ToolPresets = ToolPresets(listOf(InkStyle(0xff2255cc.toInt(), 2f),
        InkStyle(0xff222222.toInt(), 1f), InkStyle(0xffd32f2f.toInt(), 2f))),
    val highlighter: ToolPresets = ToolPresets(listOf(InkStyle(0xffffdf00.toInt(), 12f),
        InkStyle(0xff43a047.toInt(), 12f), InkStyle(0xffe754a4.toInt(), 12f))),
) {
    init {
        require(pen.slots.all { it.width in 0.5f..8f })
        require(highlighter.slots.all { it.width in 4f..32f })
    }
    fun presets(kind: AnnotationKind): ToolPresets = when (kind) {
        AnnotationKind.PEN -> pen
        AnnotationKind.HIGHLIGHTER -> highlighter
        else -> error("La herramienta no tiene estilos de tinta.")
    }
    fun save(kind: AnnotationKind, index: Int, style: InkStyle): InkPreferences = when (kind) {
        AnnotationKind.PEN -> copy(pen = pen.save(index, style))
        AnnotationKind.HIGHLIGHTER -> copy(highlighter = highlighter.save(index, style))
        else -> error("La herramienta no tiene estilos de tinta.")
    }
}
