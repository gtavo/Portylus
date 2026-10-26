package com.gtavoapps.portylus.input

// Values are intentionally independent from Android, so safety rules run on the JVM.
enum class Tool { FINGER, STYLUS, ERASER, UNKNOWN }
enum class InputAction { NAVIGATE, PEN, HIGHLIGHT, IGNORE }
data class ToolState(val pen: Boolean = false, val highlighter: Boolean = false, val fingerWriting: Boolean = false)

object InputPolicy {
    fun action(tool: Tool, state: ToolState, fingers: Int, stylusDown: Boolean): InputAction = when {
        tool == Tool.STYLUS -> if (state.pen) InputAction.PEN else InputAction.IGNORE
        tool != Tool.FINGER -> InputAction.IGNORE
        stylusDown -> InputAction.IGNORE
        fingers >= 2 -> InputAction.NAVIGATE
        state.highlighter -> InputAction.HIGHLIGHT
        state.pen && state.fingerWriting -> InputAction.PEN
        else -> InputAction.NAVIGATE
    }
}
