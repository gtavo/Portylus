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
    @Test fun fingerNavigatesWhenFingerWritingIsDisabled() {
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
    @Test fun fingerWritesOnlyWhenBothPreferenceAndPenAreEnabled() {
        for (pen in listOf(false, true)) for (finger in listOf(false, true)) {
            assertEquals(if (pen && finger) InputAction.PEN else InputAction.NAVIGATE,
                InputPolicy.action(Tool.FINGER, ToolState(pen = pen, fingerWriting = finger), 1, false))
        }
    }
    @Test fun twoFingersAndStylusPalmProtectionOverrideFingerWriting() {
        val state = ToolState(pen = true, fingerWriting = true)
        assertEquals(InputAction.NAVIGATE, InputPolicy.action(Tool.FINGER, state, 2, false))
        assertEquals(InputAction.IGNORE, InputPolicy.action(Tool.FINGER, state, 1, true))
        assertEquals(InputAction.IGNORE, InputPolicy.action(Tool.UNKNOWN, state, 1, false))
    }
    @Test fun possiblePalmCannotHighlightWhileStylusIsDown() {
        assertEquals(InputAction.IGNORE, InputPolicy.action(Tool.FINGER, ToolState(true, true), 1, true))
    }
    @Test fun unknownAndEraserNeverDrawOrDeleteAutomatically() {
        for (tool in listOf(Tool.UNKNOWN, Tool.ERASER))
            assertEquals(InputAction.IGNORE, InputPolicy.action(tool, ToolState(true, true), 1, false))
    }
}
