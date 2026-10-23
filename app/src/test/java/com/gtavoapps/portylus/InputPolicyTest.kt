package com.gtavoapps.portylus

import com.gtavoapps.portylus.input.InputAction
import com.gtavoapps.portylus.input.InputPolicy
import com.gtavoapps.portylus.input.Tool
import com.gtavoapps.portylus.input.ToolState
import org.junit.Assert.assertEquals
import org.junit.Test

class InputPolicyTest {
    @Test fun everyDocumentStartsWithNoDrawingTools() {
        val state = ToolState()
        assertEquals(InputAction.NAVIGATE, InputPolicy.action(Tool.FINGER, state, 1, false))
        assertEquals(InputAction.IGNORE, InputPolicy.action(Tool.STYLUS, state, 0, true))
    }
    @Test fun fingerCanNeverBecomePenAcrossAllModes() {
        for (pen in listOf(false, true)) for (highlight in listOf(false, true)) {
            val state = ToolState(pen, highlight)
            assertEquals(if (highlight) InputAction.HIGHLIGHT else InputAction.NAVIGATE,
                InputPolicy.action(Tool.FINGER, state, 1, false))
        }
    }
    @Test fun stylusCanNeverBecomeHighlighterAcrossAllModes() {
        for (pen in listOf(false, true)) for (highlight in listOf(false, true)) {
            assertEquals(if (pen) InputAction.PEN else InputAction.IGNORE,
                InputPolicy.action(Tool.STYLUS, ToolState(pen, highlight), 1, true))
        }
    }
    @Test fun twoFingersOverrideHighlighter() {
        assertEquals(InputAction.NAVIGATE, InputPolicy.action(Tool.FINGER, ToolState(true, true), 2, false))
    }
    @Test fun possiblePalmCannotHighlightWhileStylusIsDown() {
        assertEquals(InputAction.IGNORE, InputPolicy.action(Tool.FINGER, ToolState(true, true), 1, true))
    }
    @Test fun unknownAndEraserNeverDrawOrDeleteAutomatically() {
        for (tool in listOf(Tool.UNKNOWN, Tool.ERASER))
            assertEquals(InputAction.IGNORE, InputPolicy.action(tool, ToolState(true, true), 1, false))
    }
}
