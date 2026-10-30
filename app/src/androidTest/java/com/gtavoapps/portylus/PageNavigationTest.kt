package com.gtavoapps.portylus

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.ui.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PageNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun <T> main(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
    private fun await(block: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < until) { if (main(block)) return; SystemClock.sleep(30) }
        error("Page navigation did not reach expected state")
    }
    private fun node(label: String, root: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == label || root.contentDescription?.toString() == label)) return root
        for (i in 0 until root.childCount) node(label, root.getChild(i))?.let { return it }
        return null
    }
    private fun visible(label: String): AccessibilityNodeInfo {
        val until = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < until) {
            instrumentation.waitForIdleSync(); instrumentation.uiAutomation.clearCache()
            node(label)?.let { return it }; SystemClock.sleep(30)
        }
        error("Not visible: $label")
    }
    private fun click(label: String) {
        var target: AccessibilityNodeInfo? = visible(label)
        while (target != null && !target.isClickable) target = target.parent
        assertTrue("Cannot click $label", target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        instrumentation.waitForIdleSync()
    }
    private fun editable(root: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && root.isEditable) return root
        for (i in 0 until root.childCount) editable(root.getChild(i))?.let { return it }
        return null
    }
    private fun type(value: String) {
        visible("Ir a la página")
        val until = SystemClock.uptimeMillis() + 10000
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        while (SystemClock.uptimeMillis() < until) {
            instrumentation.waitForIdleSync(); instrumentation.uiAutomation.clearCache()
            if (editable()?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) == true) {
                while (SystemClock.uptimeMillis() < until) {
                    instrumentation.waitForIdleSync(); instrumentation.uiAutomation.clearCache()
                    if (editable()?.text?.toString().orEmpty() == value) return
                    SystemClock.sleep(30)
                }
            }
            SystemClock.sleep(30)
        }
        error("Page field is not editable")
    }
    private fun disabled(label: String) {
        val until = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < until) {
            instrumentation.waitForIdleSync(); instrumentation.uiAutomation.clearCache()
            var target = node(label)
            while (target != null) {
                if (!target.isEnabled) return
                target = target.parent
            }
            SystemClock.sleep(30)
        }
        error("Control should be disabled: $label")
    }
    private fun reader(view: View): PdfReaderView? {
        if (view is PdfReaderView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) reader(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync(); SystemClock.sleep(180)
        val image = instrumentation.uiAutomation.takeScreenshot()
        val width = instrumentation.targetContext.resources.configuration.screenWidthDp
        File(instrumentation.targetContext.getExternalFilesDir(null), "page-navigation-$width-$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
    private fun fixture(pages: Int, shortLast: Boolean = false): File {
        val objects = arrayListOf("<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Count $pages /Kids [${(0 until pages).joinToString(" ") { "${4 + it * 2} 0 R" }}] >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        repeat(pages) { index ->
            val height = if (shortLast && index == pages - 1) 80 else 700
            val content = "BT /F1 16 Tf 30 ${height - 40} Td (PDF page ${index + 1}) Tj ET\n"
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 $height] /Resources << /Font << /F1 3 0 R >> >> /Contents ${5 + index * 2} 0 R >>")
            objects.add("<< /Length ${content.length} >>\nstream\n${content}endstream")
        }
        val pdf = StringBuilder("%PDF-1.7\n")
        val offsets = objects.mapIndexed { i, obj -> pdf.length.also { pdf.append("${i + 1} 0 obj\n$obj\nendobj\n") } }
        val xref = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { pdf.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return File.createTempFile("page-navigation-", ".pdf", instrumentation.targetContext.filesDir).apply {
            writeBytes(pdf.toString().toByteArray(Charsets.US_ASCII))
        }
    }
    private fun counter(page: Int, total: Int) = "Página $page de $total. Ir a la página."

    @Test fun longPdfCounterValidatesJumpsRecreatesAndPreservesPendingInkAndOriginal() {
        val context = instrumentation.targetContext
        var activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = fixture(240); val original = source.readBytes()
        try {
            await { !model.loading && model.stylesReady }
            main { model.openPdf(Uri.fromFile(source), "Documento de 240 páginas.pdf") }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            val document = main { requireNotNull(model.document) }
            main { document.pen.add(Annotation(page = 0, kind = AnnotationKind.PEN,
                points = listOf(PagePoint(30f, 70f), PagePoint(120f, 80f)))) }
            click(counter(1, 240)); visible("Ir a la página")
            for (invalid in listOf("", "0", "-1", "abc", "241", "999999999999999999999999", "2.5")) {
                type(invalid)
                disabled("Ir")
                assertEquals(1, main { document.lastPage })
            }
            click("Cancelar"); visible(counter(1, 240))
            click("Bolígrafo"); await { reader(activity.window.decorView)?.penEnabled == true }
            click(counter(1, 240)); type("128"); screenshot("dialog-128"); click("Ir")
            await { document.lastPage == 128 }
            visible(counter(128, 240))
            assertEquals(127, main { reader(activity.window.decorView)!!.pageAt(100f, 40f)!!.first.page })
            assertFalse(main { reader(activity.window.decorView)!!.penEnabled })
            screenshot("page-128")
            click("Ocultar barra"); click(counter(128, 240)); type("240"); click("Ir")
            await { document.lastPage == 240 }; visible("Mostrar barra"); visible(counter(240, 240))
            click(counter(240, 240)); type("150")
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            try {
                main { activity.recreate() }
                activity = requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                visible("Ir a la página"); assertEquals("150", editable()?.text?.toString())
                click("Ir"); await { document.lastPage == 150 }; visible(counter(150, 240))
            } finally { instrumentation.removeMonitor(monitor) }
            click("Mostrar barra"); click("Buscar texto")
            main { model.search.query("PDF") }
            await { !model.search.state.busy && model.search.state.matches.size == 240 }
            click(counter(main { document.lastPage }, 240)); type("1"); click("Ir")
            await { !model.search.state.open && document.lastPage == 1 }
            visible(counter(1, 240)); SystemClock.sleep(300)
            assertEquals(1, main { document.lastPage })
            assertTrue(main { document.pen.isDirty })
            assertEquals(1, main { document.pen.strokes(0).size })
            assertArrayEquals(original, source.readBytes())
        } finally { main { model.closePdf(); activity.finish() }; source.delete() }
    }

    @Test fun arrowsRespectBoundsKeepZoomAndInkAndWorkWithHiddenToolbarAndSearch() {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = fixture(3, shortLast = true)
        val original = source.readBytes()
        try {
            await { !model.loading && model.stylesReady }
            main { model.openPdf(Uri.fromFile(source)) }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            val document = main { requireNotNull(model.document) }
            val view = main { requireNotNull(reader(activity.window.decorView)) }
            disabled("Página anterior")
            main { document.pen.add(Annotation(page = 0, kind = AnnotationKind.PEN,
                points = listOf(PagePoint(30f, 70f), PagePoint(120f, 80f)))) }
            // Use explicit finger events, as with real two-finger zoom input.
            val center = main { view.width / 2f }
            val span = main { maxOf(android.view.ViewConfiguration.get(context).scaledMinimumScalingSpan * 1.1f,
                view.width * 0.35f) }
            val downTime = SystemClock.uptimeMillis()
            fun touch(action: Int, factor: Float, count: Int) {
                val properties = Array(count) { index -> MotionEvent.PointerProperties().apply {
                    id = index; toolType = MotionEvent.TOOL_TYPE_FINGER
                } }
                val coords = Array(count) { index -> MotionEvent.PointerCoords().apply {
                    x = center + (if (index == 0) -1 else 1) * span * factor / 2f
                    y = 100f; pressure = 0.5f; size = 0.1f
                } }
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                    count, properties, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
                try { main { view.dispatchTouchEvent(event) } } finally { event.recycle() }
                SystemClock.sleep(30)
            }
            touch(MotionEvent.ACTION_DOWN, 1f, 1)
            touch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 1f, 2)
            touch(MotionEvent.ACTION_MOVE, 1.3f, 2)
            touch(MotionEvent.ACTION_MOVE, 1.6f, 2)
            touch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 1.6f, 2)
            touch(MotionEvent.ACTION_UP, 1.6f, 1)
            val scale = main { view.pageAt(view.width / 2f, 100f)!!.first.scale }
            assertTrue("Two-finger input must zoom", scale > view.width / 300f)
            click("Bolígrafo"); await { view.penEnabled }
            click("Página siguiente"); await { document.lastPage == 2 }
            visible(counter(2, 3)); assertFalse(main { view.penEnabled })
            assertEquals(scale, main { view.pageAt(view.width / 2f, 100f)!!.first.scale }, 0.001f)
            click("Ocultar barra"); click("Página siguiente")
            await { document.lastPage == 3 }; visible(counter(3, 3)); visible("Mostrar barra")
            disabled("Página siguiente"); screenshot("arrows-last-hidden")
            click("Página anterior"); await { document.lastPage == 2 }
            click(counter(2, 3)); type("1"); click("Ir")
            await { document.lastPage == 1 }; disabled("Página anterior")
            click("Mostrar barra"); click("Buscar texto")
            main { model.search.query("PDF") }
            await { !model.search.state.busy && model.search.state.matches.size == 3 }
            // Search may focus another page, so return to the first result explicitly.
            main { view.goToPage(1) }; visible(counter(1, 3))
            click("Página siguiente")
            await { !model.search.state.open && document.lastPage == 2 }
            assertTrue(main { document.pen.isDirty })
            assertEquals(1, main { document.pen.strokes(0).size })
            assertArrayEquals(original, source.readBytes())
            screenshot("arrows-pending-ink")
        } finally { main { model.closePdf(); activity.finish() }; source.delete() }
    }

    @Test fun singlePageDisablesBothArrows() {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = fixture(1)
        try {
            await { !model.loading }; main { model.openPdf(Uri.fromFile(source)) }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            disabled("Página anterior"); disabled("Página siguiente")
            click(counter(1, 1)); visible("Ir a la página"); click("Cancelar")
            assertEquals(1, main { model.document!!.lastPage })
        } finally { main { model.closePdf(); activity.finish() }; source.delete() }
    }

    @Test fun downloadsGuidanceOpensSystemFileAndFolderPickersWithoutAddingARoot() {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        try {
            await { !model.loading }
            val original = main { model.settings.roots }
            for (action in listOf("Abrir PDF de Descargas", "Añadir subcarpeta")) {
                instrumentation.uiAutomation.clearCache()
                if (node("¿Cómo añadir Descargas?") == null) click("Mostrar carpetas")
                click("¿Cómo añadir Descargas?")
                visible("Descargas"); screenshot("downloads-help")
                click(action)
                val deadline = SystemClock.uptimeMillis() + 10000
                while (SystemClock.uptimeMillis() < deadline &&
                    !instrumentation.uiAutomation.rootInActiveWindow?.packageName.toString().endsWith("documentsui")) {
                    SystemClock.sleep(50)
                }
                assertTrue("System picker must be open",
                    instrumentation.uiAutomation.rootInActiveWindow?.packageName.toString().endsWith("documentsui"))
                screenshot(if (action == "Añadir subcarpeta") "downloads-tree-picker" else "downloads-file-picker")
                // Back first climbs the picker's initial folder, then dismisses it.
                repeat(6) {
                    instrumentation.uiAutomation.clearCache()
                    if (instrumentation.uiAutomation.rootInActiveWindow?.packageName.toString().endsWith("documentsui")) {
                        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                        SystemClock.sleep(200)
                    }
                }
                instrumentation.waitForIdleSync()
                assertEquals(original, main { model.settings.roots })
            }
            if (context.resources.configuration.screenWidthDp < 800) {
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                instrumentation.waitForIdleSync()
            }
            visible("Abrir PDF"); screenshot("open-pdf-label")
        } finally { main { activity.finish() } }
    }

    @Test fun shortFinalPageKeepsTheRequestedCounterUntilTheReaderScrolls() {
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = fixture(3, shortLast = true)
        try {
            await { !model.loading }; main { model.openPdf(Uri.fromFile(source)) }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            click(counter(1, 3)); type("3"); click("Ir")
            await { model.document?.lastPage == 3 }; visible(counter(3, 3))
            SystemClock.sleep(300); assertEquals(3, main { model.document!!.lastPage })
            val view = main { requireNotNull(reader(activity.window.decorView)) }
            assertEquals(2, main { view.pageAt(view.width / 2f, view.height - 24f)!!.first.page })
            screenshot("short-last")
            val down = SystemClock.uptimeMillis()
            fun event(action: Int, y: Float, time: Long) {
                val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
                val coords = arrayOf(MotionEvent.PointerCoords().apply { x = 150f; this.y = y; pressure = 0.5f; size = 0.1f })
                val e = MotionEvent.obtain(down, down + time, action, 1, properties, coords,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
                try { main { view.dispatchTouchEvent(e) } } finally { e.recycle() }
            }
            event(MotionEvent.ACTION_DOWN, 150f, 0)
            event(MotionEvent.ACTION_MOVE, 330f, 300)
            event(MotionEvent.ACTION_CANCEL, 330f, 400)
            await { model.document?.lastPage == 2 }; visible(counter(2, 3))
            assertFalse(main { model.document!!.pen.isDirty })
        } finally { main { model.closePdf(); activity.finish() }; source.delete() }
    }
}
