package com.gtavoapps.portylus

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.SystemClock
import android.os.Looper
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.annotations.InkPreferences
import com.gtavoapps.portylus.annotations.InkStyle
import com.gtavoapps.portylus.annotations.EraserMode
import com.gtavoapps.portylus.annotations.EraserOptions
import com.gtavoapps.portylus.preferences.InkStylePreferences
import kotlinx.coroutines.runBlocking
import android.content.ClipboardManager
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.ui.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Actual reader + Compose toolbar. Synthetic native events do not replace physical pen tests. */
@RunWith(AndroidJUnit4::class)
class PenInputTest {
    @Test fun externalContentViewResolvesOpensColdAndWarmAndProtectsUnsavedInk() {
        val uri = Uri.parse("content://com.gtavoapps.portylus.test.fixture/sample.pdf")
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addCategory(Intent.CATEGORY_DEFAULT).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setPackage(instrumentation.targetContext.packageName)
        val matches = instrumentation.targetContext.packageManager.queryIntentActivities(intent, 0)
        assertTrue(matches.any { it.activityInfo.name == MainActivity::class.java.name })
        assertEquals("Portylus", instrumentation.targetContext.applicationInfo.loadLabel(instrumentation.targetContext.packageManager))
        val activity = instrumentation.startActivitySync(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        try {
            await { model.document?.name == "Externo.pdf" && findReader(activity.window.decorView)?.isDocumentReady == true }
            val positions = com.gtavoapps.portylus.preferences.ReadingPositions(instrumentation.targetContext)
            val other = Uri.parse("content://com.gtavoapps.portylus.test.fixture/other.pdf")
            main {
                model.recordPage(requireNotNull(model.document), 2)
                model.document!!.pen.add(Annotation(page = 0, kind = AnnotationKind.PEN,
                    points = listOf(PagePoint(10f, 10f), PagePoint(20f, 20f))))
            }
            val opened = main { model.document }
            instrumentation.targetContext.startActivity(Intent(intent).setDataAndType(other, "application/pdf")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            await { model.pendingExternalUri == other }
            assertSame(opened, main { model.document })
            click("Cancelar")
            await { model.pendingExternalUri == null }
            assertTrue(main { model.document!!.pen.hasStrokes })
            instrumentation.targetContext.startActivity(Intent(intent).setDataAndType(other, "application/pdf")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            await { model.pendingExternalUri == other }
            click("No guardar")
            await { model.document !== opened && model.document != null && !model.opening }
            assertFalse(main { model.document!!.pen.hasStrokes })
            assertEquals(2, positions.read(positions.key(uri), 2))
            assertEquals(1, main { model.document!!.lastPage })
            val tree = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload")
            val treeDoc = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, "primary:Download/test.pdf")
            val directDoc = android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/test.pdf")
            assertEquals(positions.key(directDoc), positions.key(treeDoc))
            assertTrue(activity.window.isNavigationBarContrastEnforced)
            assertFalse(main { androidx.core.view.WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                .isAppearanceLightNavigationBars })
        } finally { main { model.closePdf(); activity.finish() }; instrumentation.waitForIdleSync() }
    }

    @Test fun forgettingLocationPersistsWithoutDeletingSourceFiles() {
        val preferences = com.gtavoapps.portylus.preferences.FolderPreferences(instrumentation.targetContext)
        val previous = runBlocking { preferences.read() }
        val root = com.gtavoapps.portylus.explorer.FolderLocation("content://com.gtavoapps.portylus.test.fixture/tree/sample",
            listOf(com.gtavoapps.portylus.explorer.FolderRef("content://com.gtavoapps.portylus.test.fixture/document/sample", "Prueba")))
        try {
            runBlocking { preferences.write(com.gtavoapps.portylus.explorer.BrowserPreferences(listOf(root), listOf(root), root)) }
            Reader().use { reader ->
                main { reader.model.removeRoot(root.treeUri) }
                await { reader.model.settings.roots.isEmpty() && reader.model.location == null }
                val saved = runBlocking { preferences.read() }
                assertTrue(saved.roots.isEmpty()); assertTrue(saved.bookmarks.isEmpty()); assertNull(saved.lastLocation)
                assertTrue(reader.source.exists())
                assertArrayEquals(reader.original, reader.source.readBytes())
            }
        } finally { runBlocking { preferences.write(previous) } }
    }

    @Test fun eraserPreviewCancelsOnSecondFingerAndCommitsOnlySelectedInk() {
        Reader().use { reader ->
            main {
                val page = requireNotNull(reader.view.pageAt(100f, 150f)).first
                val a = requireNotNull(page.toPage(80f, 150f))
                val b = requireNotNull(page.toPage(300f, 150f))
                val pen = requireNotNull(reader.model.document).pen
                pen.add(Annotation(page = 0, kind = AnnotationKind.PEN, points = listOf(a, b)))
                pen.add(Annotation(page = 0, kind = AnnotationKind.HIGHLIGHTER, points = listOf(a, b)))
            }
            click("Goma")
            await { reader.view.eraserEnabled }
            val originals = reader.strokes()
            reader.send(MotionEvent.ACTION_DOWN, finger(190f, 150f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                finger(190f, 150f), finger(230f, 180f, 3))
            reader.send(MotionEvent.ACTION_CANCEL, finger(190f, 150f), finger(230f, 180f, 3))
            assertEquals(originals, reader.strokes())
            reader.send(MotionEvent.ACTION_DOWN, stylus(190f, 110f))
            reader.send(MotionEvent.ACTION_MOVE, stylus(190f, 190f))
            reader.send(MotionEvent.ACTION_UP, stylus(190f, 190f))
            assertEquals(2, reader.strokes().count { it.kind == AnnotationKind.PEN })
            assertEquals(originals.last(), reader.strokes().last())
            click("Opciones de goma"); click("Trazo completo"); click("Resaltados"); click("Cerrar")
            // Events are dispatched directly to the reader: wait until the modal has actually returned its focus.
            await { reader.view.hasWindowFocus() && reader.view.eraserOptions == EraserOptions(EraserMode.WHOLE, AnnotationKind.HIGHLIGHTER) }
            reader.send(MotionEvent.ACTION_DOWN, finger(190f, 150f))
            reader.send(MotionEvent.ACTION_UP, finger(190f, 150f))
            assertEquals(2, reader.strokes().size)
            assertTrue(reader.strokes().all { it.kind == AnnotationKind.PEN })
            click("Opciones de goma"); click("Escritura"); click("Cerrar")
            await { reader.view.hasWindowFocus() && reader.view.eraserOptions == EraserOptions(EraserMode.WHOLE) }
            // Deliver consecutive gestures in one main-thread turn, then the queued toolbar revision.
            main {
                for (x in listOf(100f, 280f)) {
                    reader.send(MotionEvent.ACTION_DOWN, finger(x, 150f))
                    reader.view.annotationRevision = requireNotNull(reader.model.document).pen.revision
                    reader.send(MotionEvent.ACTION_UP, finger(x, 150f))
                }
            }
            assertTrue("Goma con foco=" + main { reader.view.hasWindowFocus() } +
                ", opciones=" + main { reader.view.eraserOptions } + ", restos=" + reader.strokes(),
                reader.strokes().isEmpty())
            assertFalse(main { requireNotNull(reader.model.document).pen.hasStrokes })
        }
    }

    @Test fun delayedToolbarRevisionCannotCancelTheNextPenGestureButUndoStillCancelsItsPreview() {
        Reader().use { reader ->
            reader.enable()
            main {
                reader.write()
                reader.send(MotionEvent.ACTION_DOWN, stylus(250f, 150f))
                // Reproduce AndroidView applying the previous commit after the next DOWN.
                reader.view.annotationRevision = requireNotNull(reader.model.document).pen.revision
                reader.send(MotionEvent.ACTION_UP, stylus(300f, 150f))
            }
            assertEquals(2, reader.strokes().size)
            main {
                reader.send(MotionEvent.ACTION_DOWN, stylus(350f, 150f))
                val pen = requireNotNull(reader.model.document).pen
                pen.undo()
                reader.view.annotationRevision = pen.revision
                reader.send(MotionEvent.ACTION_UP, stylus(400f, 150f))
            }
            assertEquals(1, reader.strokes().size)
        }
    }

    @Test fun readingPageRestoresAfterCloseAndNewModelAndNewFilesStartAtOne() {
        Reader().use { reader ->
            val positions = com.gtavoapps.portylus.preferences.ReadingPositions(instrumentation.targetContext)
            val key = positions.key(Uri.fromFile(reader.source))
            val oldView = reader.view
            main {
                reader.model.recordPage(requireNotNull(reader.model.document), 2)
                reader.model.closePdf()
                reader.model.openPdf(Uri.fromFile(reader.source))
            }
            // The model can publish the reopened document before Compose replaces the old view.
            await { reader.model.document?.lastPage == 2 && !reader.model.opening &&
                findReader(reader.activity.window.decorView)?.let {
                    it !== oldView && it.isDocumentReady && it.width > 0 && it.height > 0
                } == true }
            assertEquals(1, main { reader.view.pageAt(100f, 150f)!!.first.page })
            assertEquals(2, positions.read(key, 2))
            assertEquals(1, positions.read("new-file-${System.nanoTime()}", 2))
            assertEquals(1, positions.read(key, 1))
            main { reader.model.closePdf(); reader.activity.finish() }
            instrumentation.waitForIdleSync()
            reader.activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            reader.model = main { ViewModelProvider(reader.activity)[AppViewModel::class.java] }
            await { !reader.model.loading }
            main { reader.model.openPdf(Uri.fromFile(reader.source)) }
            await { reader.model.document?.lastPage == 2 && findReader(reader.activity.window.decorView)?.isDocumentReady == true }
            assertEquals(1, main { reader.view.pageAt(100f, 150f)!!.first.page })
            assertFalse(main { reader.view.eraserEnabled || reader.view.penEnabled || reader.view.highlighterEnabled })
        }
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private data class Pointer(val id: Int, val type: Int, val x: Float, val y: Float)
    private fun stylus(x: Float, y: Float, id: Int = 7) = Pointer(id, MotionEvent.TOOL_TYPE_STYLUS, x, y)
    private fun finger(x: Float, y: Float, id: Int = 2) = Pointer(id, MotionEvent.TOOL_TYPE_FINGER, x, y)

    private fun <T> main(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            if (main(condition)) return
            SystemClock.sleep(25)
        }
        error("El lector no alcanzó el estado esperado.")
    }
    private fun findReader(view: View): PdfReaderView? {
        if (view is PdfReaderView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount)
            findReader(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun click(text: String) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if ((node.text?.toString() == text || node.contentDescription?.toString() == text) && node.isVisibleToUser) return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.clearCache()
            val root = instrumentation.uiAutomation.rootInActiveWindow
            // Compose exposes virtual nodes; walk them instead of relying on platform text indexing.
            val node = find(root)
            if (node != null) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) target = target.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
            }
            SystemClock.sleep(50)
        }
        error("No se encuentra el control $text")
    }

    private fun setWidth(value: Float) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            // Slider may expose its label and range on different virtual accessibility nodes.
            if (node.rangeInfo != null && node.isVisibleToUser) return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.clearCache()
            val slider = find(instrumentation.uiAutomation.rootInActiveWindow)
            val args = Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value) }
            if (slider?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, args) == true) {
                while (SystemClock.uptimeMillis() < deadline) {
                    instrumentation.uiAutomation.clearCache()
                    val current = find(instrumentation.uiAutomation.rootInActiveWindow)?.rangeInfo?.current
                    if (current != null && kotlin.math.abs(current - value) < 0.001f) return
                    SystemClock.sleep(25)
                }
                break
            }
            SystemClock.sleep(25)
        }
        error("No se puede ajustar el grosor a $value")
    }

    private fun configure(kind: String, slot: Int, color: String, width: Float) {
        click("Estilos del $kind")
        click("Estilo ${slot + 1}")
        fun selected(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.text?.toString() == "Estilo ${slot + 1}") {
                var parent: AccessibilityNodeInfo? = node
                while (parent != null) {
                    if (parent.isSelected || parent.isChecked) return true
                    parent = parent.parent
                }
            }
            for (index in 0 until node.childCount) if (selected(node.getChild(index))) return true
            return false
        }
        val deadline = SystemClock.uptimeMillis() + 5000
        while (true) {
            instrumentation.uiAutomation.clearCache()
            if (selected(instrumentation.uiAutomation.rootInActiveWindow)) break
            if (SystemClock.uptimeMillis() >= deadline) error("La memoria no se ha seleccionado")
            SystemClock.sleep(25)
        }
        click("Color $color")
        setWidth(width)
        click("Guardar y usar")
    }

    private fun setHex(text: String) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && node.isVisibleToUser) return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.clearCache()
            val field = find(instrumentation.uiAutomation.rootInActiveWindow)
            val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            if (field?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args) == true) {
                while (SystemClock.uptimeMillis() < deadline) {
                    instrumentation.uiAutomation.clearCache()
                    if (find(instrumentation.uiAutomation.rootInActiveWindow)?.text?.toString()?.contains(text) == true) return
                    SystemClock.sleep(25)
                }
            }
            SystemClock.sleep(25)
        }
        error("No se puede editar el color")
    }

    private inner class Reader(resetStyles: Boolean = true, displayName: String = "Prueba.pdf") : AutoCloseable {
        private val defaults = if (resetStyles) runBlocking {
            InkStylePreferences(instrumentation.targetContext).write(InkPreferences())
        } else Unit
        var activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        var model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        private val previousFingerWriting = main { model.fingerWriting }
        val source = File.createTempFile("pen-input-", ".pdf", instrumentation.targetContext.filesDir).apply {
            instrumentation.context.assets.open("base.pdf").use { input -> outputStream().use(input::copyTo) }
        }
        val original = source.readBytes()
        val view: PdfReaderView get() = main { requireNotNull(findReader(activity.window.decorView)) }
        var eventTime = SystemClock.uptimeMillis()
        var downTime = eventTime

        init {
            await { !model.loading && model.stylesReady }
            // These historical stylus/palm fixtures explicitly exercise stylus-only mode.
            main { model.updateFingerWriting(false) }
            main { model.openPdf(Uri.fromFile(source), displayName) }
            await { model.document != null && !model.opening && findReader(activity.window.decorView)?.isDocumentReady == true }
            instrumentation.waitForIdleSync()
        }
        fun enable() {
            click("Bolígrafo")
            await { view.penEnabled }
        }
        fun send(action: Int, vararg pointers: Pointer, flags: Int = 0) {
            if (action == MotionEvent.ACTION_DOWN) downTime = SystemClock.uptimeMillis().also { eventTime = it }
            eventTime += 16
            val properties = pointers.map { point -> MotionEvent.PointerProperties().apply {
                id = point.id; toolType = point.type
            } }.toTypedArray()
            val coords = pointers.map { point -> MotionEvent.PointerCoords().apply {
                x = point.x; y = point.y; pressure = 0.6f; size = 0.1f
            } }.toTypedArray()
            val event = MotionEvent.obtain(downTime, eventTime, action, pointers.size, properties,
                coords, 0, 0, 1f, 1f, 0, 0,
                if (pointers.any { it.type == MotionEvent.TOOL_TYPE_STYLUS }) InputDevice.SOURCE_STYLUS else InputDevice.SOURCE_TOUCHSCREEN,
                flags)
            try { main { view.dispatchTouchEvent(event) } } finally { event.recycle() }
        }
        fun write() {
            send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            send(MotionEvent.ACTION_MOVE, stylus(150f, 170f))
            send(MotionEvent.ACTION_UP, stylus(200f, 150f))
        }
        fun strokes(): List<Annotation> = main { requireNotNull(model.document).pen.strokes(0).toList() }
        fun assertInkAt(x: Float, y: Float) {
            val bitmap = main {
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
            try { assertEquals(0xff2255cc.toInt(), bitmap.getPixel(x.toInt(), y.toInt())) }
            finally { bitmap.recycle() }
        }
        fun pdfTextPixelCount(): Int = main {
            val placement = requireNotNull(view.pageAt(view.width / 2f, 200f)).first
            val a = placement.toScreen(PagePoint(30f, 44f))
            val b = placement.toScreen(PagePoint(180f, 66f))
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                var pixels = 0
                for (y in a.y.toInt().coerceAtLeast(0) until b.y.toInt().coerceAtMost(view.height))
                    for (x in a.x.toInt().coerceAtLeast(0) until b.x.toInt().coerceAtMost(view.width)) {
                        val color = bitmap.getPixel(x, y)
                        if (android.graphics.Color.red(color) < 100 && android.graphics.Color.green(color) < 100 &&
                            android.graphics.Color.blue(color) < 100) pixels++
                    }
                pixels
            } finally { bitmap.recycle() }
        }
        override fun close() {
            assertArrayEquals("El bolígrafo no debe modificar el documento original", original, source.readBytes())
            main { model.updateFingerWriting(previousFingerWriting); model.closePdf(); activity.finish() }
            instrumentation.waitForIdleSync()
            source.delete()
        }
    }

    @Test fun responsiveToolbarKeepsActionsVisibleAndAllPrimaryToolsReachable() {
        val name = "Álgebra y geometría · apuntes del primer trimestre con un nombre muy largo.pdf"
        fun find(label: String, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isVisibleToUser && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
            for (i in 0 until node.childCount) find(label, node.getChild(i))?.let { return it }
            return null
        }
        fun bounds(label: String): android.graphics.Rect {
            val limit = SystemClock.uptimeMillis() + 10000
            while (SystemClock.uptimeMillis() < limit) {
                instrumentation.uiAutomation.clearCache()
                find(label)?.let { node -> return android.graphics.Rect().also { node.getBoundsInScreen(it) } }
                SystemClock.sleep(30)
            }
            error("Control no visible: " + label)
        }
        fun revealTool(label: String) {
            fun scrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (node == null) return null
                if (node.isScrollable && node.isVisibleToUser) return node
                for (i in 0 until node.childCount) scrollable(node.getChild(i))?.let { return it }
                return null
            }
            val deadline = SystemClock.uptimeMillis() + 10000
            while (SystemClock.uptimeMillis() < deadline) {
                instrumentation.uiAutomation.clearCache()
                if (find(label) != null) return
                scrollable(instrumentation.uiAutomation.rootInActiveWindow)
                    ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                instrumentation.waitForIdleSync()
                SystemClock.sleep(80)
            }
            error("Herramienta fuera de la franja: " + label)
        }
        fun screenshot(label: String) {
            instrumentation.waitForIdleSync()
            SystemClock.sleep(150)
            val suffix = if (instrumentation.targetContext.resources.configuration.screenWidthDp < 600) "-movil" else ""
            val image = instrumentation.uiAutomation.takeScreenshot()
            File(instrumentation.targetContext.getExternalFilesDir(null), "Portylus-v0.0.10-" + label + suffix + ".png")
                .outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
            image.recycle()
        }
        Reader(displayName = name).use { reader ->
            assertEquals(name, main { reader.model.document!!.name })
            screenshot("barra-lectura")
            val title = bounds(name)
            val undo = bounds("Deshacer")
            val save = bounds("Guardar")
            assertTrue(title.right <= undo.left)
            assertTrue(undo.left < bounds("Rehacer").left && undo.right <= save.left)
            assertTrue(kotlin.math.abs(title.centerY() - save.centerY()) <= 3)
            assertTrue(title.width() <= 240 * instrumentation.targetContext.resources.displayMetrics.density)
            if (instrumentation.targetContext.resources.configuration.screenWidthDp >= 800) {
                for (label in listOf("Atrás","Bolígrafo","Estilos del bolígrafo","Fluorescente",
                    "Estilos del fluorescente","Seleccionar texto","Goma","Ocultar barra","Más opciones")) {
                    assertTrue(label, kotlin.math.abs(bounds(label).centerY() - undo.centerY()) <= 3)
                }
                assertTrue(title.right <= bounds("Bolígrafo").left)
                assertTrue(bounds("Ocultar barra").right <= undo.left)
            } else if (instrumentation.targetContext.resources.configuration.screenWidthDp < 600) {
                for (label in listOf("Bolígrafo", "Fluorescente", "Seleccionar texto", "Goma", "Ocultar barra")) {
                    assertTrue(label, bounds(label).top >= save.bottom)
                    assertTrue(label, bounds(label).width() >= 47 * instrumentation.targetContext.resources.displayMetrics.density)
                }
            }
            screenshot("barra-lectura")
            reader.enable(); reader.write()
            revealTool("Fluorescente"); click("Fluorescente")
            await { reader.view.penEnabled && reader.view.highlighterEnabled }
            assertTrue(main { reader.model.document!!.pen.isDirty })
            screenshot("barra-escritura")
            revealTool("Goma"); click("Goma")
            await { reader.view.eraserEnabled && !reader.view.penEnabled && !reader.view.highlighterEnabled }
            revealTool("Opciones de goma")
            screenshot("barra-goma")
            click("Opciones de goma"); click("Cerrar")
            await { reader.view.hasWindowFocus() }
            revealTool("Ocultar barra"); click("Ocultar barra")
            await { !reader.view.eraserEnabled }
            click("Mostrar barra")
            revealTool("Bolígrafo")
            await { !reader.view.penEnabled && !reader.view.highlighterEnabled && !reader.view.eraserEnabled }
            assertEquals(1, reader.strokes().size)
        }
    }

    @Test fun startsSafeAndOnlyStylusWritesAfterExplicitActivation() {
        Reader().use { reader ->
            assertFalse(main { reader.view.penEnabled })
            reader.write()
            assertTrue(reader.strokes().isEmpty())
            reader.enable()
            for (tool in listOf(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.TOOL_TYPE_ERASER, MotionEvent.TOOL_TYPE_UNKNOWN)) {
                reader.send(MotionEvent.ACTION_DOWN, Pointer(4, tool, 100f, 150f))
                reader.send(MotionEvent.ACTION_MOVE, Pointer(4, tool, 150f, 170f))
                reader.send(MotionEvent.ACTION_UP, Pointer(4, tool, 200f, 150f))
            }
            assertTrue(reader.strokes().isEmpty())
            val expected = main { requireNotNull(reader.view.pageAt(100f, 150f)).second }
            reader.write()
            assertEquals(1, reader.strokes().size)
            assertEquals(expected, reader.strokes().single().points.first())
            reader.assertInkAt(150f, 170f)
        }
    }

    @Test fun threePenStylesPersistAcrossNewViewModelsAndNeverRecolorExistingInk() {
        val red = InkStyle(0xffd32f2f.toInt(), 4f)
        val cyan = InkStyle(0xff00acc1.toInt(), 1.5f)
        val green = InkStyle(0xff43a047.toInt(), 6f)
        try {
            Reader().use { reader ->
                reader.enable(); reader.write()
                val original = reader.strokes().single()
                for ((slot, sample) in listOf("Rojo" to red, "Cian" to cyan, "Verde" to green).withIndex()) {
                    configure("bolígrafo", slot, sample.first, sample.second.width)
                    await { !reader.model.savingStyle && reader.view.penStyle == sample.second }
                    reader.write()
                    val stroke = reader.strokes().last()
                    assertEquals(sample.second.color, stroke.color)
                    assertEquals(sample.second.width, stroke.width, 0f)
                    assertEquals(original, reader.strokes().first())
                }
                assertEquals(listOf(red, cyan, green), main { reader.model.inkStyles.pen.slots })
                assertEquals(InkPreferences().highlighter, main { reader.model.inkStyles.highlighter })
            }
            Reader(resetStyles = false).use { reader ->
                await { reader.view.penStyle == green }
                assertFalse(main { reader.view.penEnabled || reader.view.highlighterEnabled })
                assertEquals(listOf(red, cyan, green), main { reader.model.inkStyles.pen.slots })
                click("Estilos del bolígrafo"); click("Estilo 1"); click("Guardar y usar")
                await { reader.view.penStyle == red }
                assertFalse(main { reader.view.penEnabled })
            }
        } finally { runBlocking { InkStylePreferences(instrumentation.targetContext).write(InkPreferences()) } }
    }

    @Test fun threeHighlighterStylesPersistAndKeepTheSameOpacityAndStraightLine() {
        val styles = listOf(InkStyle(0xffe754a4.toInt(), 8f), InkStyle(0xff00acc1.toInt(), 20f),
            InkStyle(0xff43a047.toInt(), 28f))
        try {
            Reader().use { reader ->
                click("Fluorescente"); await { reader.view.highlighterEnabled }
                for ((slot, name) in listOf("Rosa", "Cian", "Verde").withIndex()) {
                    val style = styles[slot]
                    configure("fluorescente", slot, name, style.width)
                    await { !reader.model.savingStyle && reader.view.highlighterStyle == style }
                    reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
                    reader.send(MotionEvent.ACTION_MOVE, finger(200f, 400f))
                    reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
                    val stroke = reader.strokes().last()
                    assertEquals(style.color, stroke.color)
                    assertEquals(style.width, stroke.width, 0f)
                    assertEquals(2, stroke.points.size)
                }
                assertEquals(styles, main { reader.model.inkStyles.highlighter.slots })
                assertEquals(InkPreferences().pen, main { reader.model.inkStyles.pen })
            }
            Reader(resetStyles = false).use { reader ->
                await { reader.view.highlighterStyle == styles.last() }
                assertFalse(main { reader.view.highlighterEnabled || reader.view.penEnabled })
                assertEquals(styles, main { reader.model.inkStyles.highlighter.slots })
            }
        } finally { runBlocking { InkStylePreferences(instrumentation.targetContext).write(InkPreferences()) } }
    }

    @Test fun cancelingStyleEditorDoesNotOverwriteMemoryOrEnableDrawing() {
        Reader().use { reader ->
            click("Estilos del bolígrafo"); click("Estilo 3"); click("Color Naranja"); setWidth(8f)
            click("Cancelar")
            assertEquals(InkPreferences().pen, main { reader.model.inkStyles.pen })
            assertFalse(main { reader.view.penEnabled })
            assertEquals(InkPreferences(), runBlocking { InkStylePreferences(instrumentation.targetContext).read() })
        }
    }

    @Test fun strokeSnapshotsItsStyleAndHighlighterOpacityRemains72OutOf255() {
        Reader().use { reader ->
            val penStyle = InkStyle(0xffd32f2f.toInt(), 6f)
            reader.enable()
            main { reader.view.penStyle = penStyle }
            reader.send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            main { reader.view.penStyle = InkStyle(0xff43a047.toInt(), 1f) }
            reader.send(MotionEvent.ACTION_UP, stylus(400f, 150f))
            assertEquals(penStyle.color, reader.strokes().single().color)
            assertEquals(penStyle.width, reader.strokes().single().width, 0f)
            val penBitmap = main { Bitmap.createBitmap(reader.view.width, reader.view.height, Bitmap.Config.ARGB_8888)
                .also { reader.view.draw(Canvas(it)) } }
            try {
                assertEquals(penStyle.color, penBitmap.getPixel(250, 160))
                assertEquals(android.graphics.Color.WHITE, penBitmap.getPixel(250, 168))
            } finally { penBitmap.recycle() }
            click("Fluorescente"); await { reader.view.highlighterEnabled }
            val style = InkStyle(0xff43a047.toInt(), 20f)
            main { reader.view.highlighterStyle = style }
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 400f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 400f))
            val bitmap = main { Bitmap.createBitmap(reader.view.width, reader.view.height, Bitmap.Config.ARGB_8888)
                .also { reader.view.draw(Canvas(it)) } }
            try {
                val expected = listOf(0x43, 0xa0, 0x47).map { (255 * (1f - 72f / 255f) + it * 72f / 255f).toInt() }
                val pixel = bitmap.getPixel(250, 400)
                for ((i, actual) in listOf(android.graphics.Color.red(pixel), android.graphics.Color.green(pixel),
                    android.graphics.Color.blue(pixel)).withIndex()) assertEquals(expected[i].toFloat(), actual.toFloat(), 1f)
            } finally { bitmap.recycle() }
        }
    }

    @Test fun customRgbColorCanBeSavedAndInvalidInputDoesNotModifyPreferences() {
        Reader().use { reader ->
            click("Estilos del bolígrafo")
            setHex("#12")
            assertEquals(InkPreferences(), runBlocking { InkStylePreferences(instrumentation.targetContext).read() })
            setHex("#123ABC"); setWidth(3f)
            click("Guardar y usar")
            val style = InkStyle(0xff123abc.toInt(), 3f)
            await { reader.view.penStyle == style }
            assertFalse(main { reader.view.penEnabled })
            reader.enable(); reader.write()
            assertEquals(style.color, reader.strokes().single().color)
            assertEquals(style.width, reader.strokes().single().width, 0f)
        }
    }

    @Test fun fingerStillNavigatesWithPenEnabledAndNeverAddsInk() {
        Reader().use { reader ->
            reader.enable()
            val before = main { requireNotNull(reader.view.pageAt(100f, 200f)).second.y }
            reader.send(MotionEvent.ACTION_DOWN, finger(300f, 400f))
            reader.send(MotionEvent.ACTION_MOVE, finger(300f, 200f))
            reader.send(MotionEvent.ACTION_UP, finger(300f, 200f))
            val after = main { requireNotNull(reader.view.pageAt(100f, 200f)).second.y }
            assertTrue(after > before)
            assertTrue(reader.strokes().isEmpty())
        }
    }

    @Test fun tracksStylusIdWhenIndicesChangeAndQuarantinesPalmTail() {
        Reader().use { reader ->
            reader.enable()
            val before = main { requireNotNull(reader.view.pageAt(100f, 200f)).second }
            reader.send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                stylus(100f, 150f), finger(400f, 450f))
            reader.send(MotionEvent.ACTION_MOVE, finger(400f, 100f), stylus(170f, 170f))
            reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                finger(400f, 100f), stylus(200f, 150f))
            reader.send(MotionEvent.ACTION_MOVE, finger(400f, 500f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 500f))
            assertEquals(1, reader.strokes().size)
            assertEquals(before, main { requireNotNull(reader.view.pageAt(100f, 200f)).second })
            assertEquals(3, reader.strokes().single().points.size)
        }
    }

    @Test fun cancellationAndDisablingNeverCommitAPartialStroke() {
        Reader().use { reader ->
            reader.enable()
            reader.send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            reader.send(MotionEvent.ACTION_MOVE, stylus(150f, 170f))
            reader.send(MotionEvent.ACTION_CANCEL, stylus(200f, 150f))
            reader.send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            reader.send(MotionEvent.ACTION_UP, stylus(200f, 150f), flags = MotionEvent.FLAG_CANCELED)
            reader.send(MotionEvent.ACTION_DOWN, stylus(100f, 150f))
            click("Bolígrafo activo")
            await { !reader.view.penEnabled }
            reader.send(MotionEvent.ACTION_UP, stylus(200f, 150f))
            assertTrue(reader.strokes().isEmpty())
        }
    }

    @Test fun canceledPalmDoesNotCancelTheLegitimateStylusStroke() {
        Reader().use { reader ->
            reader.enable()
            reader.send(MotionEvent.ACTION_DOWN, finger(400f, 450f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                finger(400f, 450f), stylus(100f, 150f))
            reader.send(MotionEvent.ACTION_POINTER_UP, finger(400f, 450f), stylus(100f, 150f),
                flags = MotionEvent.FLAG_CANCELED)
            reader.send(MotionEvent.ACTION_MOVE, stylus(150f, 170f))
            reader.send(MotionEvent.ACTION_UP, stylus(200f, 150f))
            assertEquals(1, reader.strokes().size)
        }
    }

    @Test fun twoFingerZoomKeepsInkOnItsDocumentCoordinates() {
        Reader().use { reader ->
            reader.enable(); reader.write()
            val point = reader.strokes().single().points[1]
            val center = main { reader.view.width * 0.5f }
            val span = main { maxOf(ViewConfiguration.get(reader.view.context).scaledMinimumScalingSpan * 1.1f,
                reader.view.width * 0.35f) }
            fun left(factor: Float) = finger(center - span * factor / 2f, 200f)
            fun right(factor: Float) = finger(center + span * factor / 2f, 200f, 5)
            reader.send(MotionEvent.ACTION_DOWN, left(1f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                left(1f), right(1f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.3f), right(1.3f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.6f), right(1.6f))
            reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                left(1.6f), right(1.6f))
            reader.send(MotionEvent.ACTION_UP, left(1.6f))
            val transformed = main {
                val page = requireNotNull(reader.view.pageAt(200f, 200f)).first
                assertTrue("Escala actual ${page.scale}, inicial ${reader.view.width / page.size.width}",
                    page.scale > reader.view.width / page.size.width)
                page.toScreen(point)
            }
            assertEquals(1, reader.strokes().size)
            reader.assertInkAt(transformed.x, transformed.y)
        }
    }

    @Test fun twoFingerPanKeepsPdfVisibleWhileNewResolutionIsPending() {
        Reader().use { reader ->
            await { reader.pdfTextPixelCount() > 100 }
            val session = main { reader.model.document!!.session }
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            // Hold actual native rendering to simulate a slow PDF, without timing-based sleeps.
            val blocker = Thread {
                synchronized(session) { entered.countDown(); release.await(15, TimeUnit.SECONDS) }
            }.apply { isDaemon = true; start() }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            try {
                val center = main { reader.view.width * 0.5f }
                val span = main { maxOf(ViewConfiguration.get(reader.view.context).scaledMinimumScalingSpan * 1.1f,
                    reader.view.width * 0.35f) }
                fun left(factor: Float, y: Float) = finger(center - span * factor / 2, y)
                fun right(factor: Float, y: Float) = finger(center + span * factor / 2, y, 5)
                reader.send(MotionEvent.ACTION_DOWN, left(1f, 350f))
                reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    left(1f, 350f), right(1f, 350f))
                reader.send(MotionEvent.ACTION_MOVE, left(1.10f, 340f), right(1.10f, 340f))
                reader.send(MotionEvent.ACTION_MOVE, left(1.12f, 330f), right(1.12f, 330f))
                val beforePan = main { reader.view.pageAt(center, 200f)!!.first.top }
                for (step in 1..4) {
                    reader.send(MotionEvent.ACTION_MOVE, left(1.12f, 330f - step * 10), right(1.12f, 330f - step * 10))
                    assertTrue("El contenido PDF debe seguir visible con los dedos apoyados, antes de acabar el render",
                        reader.pdfTextPixelCount() > 100)
                }
                assertTrue(main { reader.view.pageAt(center, 200f)!!.first.top } < beforePan - 20f)
                assertTrue(reader.strokes().isEmpty())
                reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                    left(1.12f, 290f), right(1.12f, 290f))
                reader.send(MotionEvent.ACTION_UP, left(1.12f, 290f))
            } finally { release.countDown(); blocker.join(5000) }
            await { reader.pdfTextPixelCount() > 100 }
        }
    }

    @Test fun twoFingerPanWithoutPinchingRedrawsWhileHeldInReadingHighlightAndSelectionModes() {
        for (mode in listOf("", "Fluorescente", "Seleccionar texto")) Reader().use { reader ->
            if (mode.isNotEmpty()) click(mode)
            await {
                when (mode) {
                    "Fluorescente" -> reader.view.highlighterEnabled
                    "Seleccionar texto" -> reader.view.textSelectionEnabled
                    else -> true
                }
            }
            await { reader.pdfTextPixelCount() > 100 }
            val span = main { maxOf(ViewConfiguration.get(reader.view.context).scaledMinimumScalingSpan * 1.1f,
                reader.view.width * 0.35f) }
            val center = main { reader.view.width * 0.5f }
            fun left(y: Float) = finger(center - span / 2f, y)
            fun right(y: Float) = finger(center + span / 2f, y, 5)
            val before = main { reader.view.pageAt(center, 200f)!!.first }
            reader.send(MotionEvent.ACTION_DOWN, left(400f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(400f), right(400f))
            for (step in 1..4) {
                val y = 400f - step * 15
                reader.send(MotionEvent.ACTION_MOVE, left(y), right(y))
                assertTrue("PDF visible durante desplazamiento sin pellizco, modo $mode", reader.pdfTextPixelCount() > 100)
            }
            val after = main { reader.view.pageAt(center, 200f)!!.first }
            assertEquals("Desplazar no debe ampliar", before.scale, after.scale, 0.001f)
            assertTrue("Desplazar antes de soltar, modo $mode", after.top < before.top - 20f)
            assertTrue(reader.strokes().isEmpty())
            reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(340f), right(340f))
            reader.send(MotionEvent.ACTION_UP, left(340f))
        }
    }

    @Test fun rotationKeepsCompletedStrokesButReturnsToSafeReadingMode() {
        Reader().use { reader ->
            reader.enable(); reader.write()
            click("Fluorescente"); await { reader.view.highlighterEnabled }
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
            val highlightPoints = reader.strokes().last().points
            val points = reader.strokes().first().points
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            try {
                main { reader.activity.recreate() }
                reader.activity = requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                reader.model = main { ViewModelProvider(reader.activity)[AppViewModel::class.java] }
                await { findReader(reader.activity.window.decorView)?.isDocumentReady == true }
                assertFalse(main { reader.view.penEnabled })
                assertFalse(main { reader.view.highlighterEnabled })
                assertFalse(main { reader.view.textSelectionEnabled })
                assertEquals(points, reader.strokes().first().points)
                assertEquals(highlightPoints, reader.strokes().last().points)
            } finally { instrumentation.removeMonitor(monitor) }
        }
    }

    @Test fun highlighterIsFingerOnlyStraightAndTranslucentEvenAfterACurvedGesture() {
        Reader().use { reader ->
            assertFalse(main { reader.view.highlighterEnabled })
            click("Fluorescente")
            await { reader.view.highlighterEnabled }
            reader.write()
            assertTrue(reader.strokes().isEmpty())
            val start = main { requireNotNull(reader.view.pageAt(100f, 300f)).second }
            val end = main { requireNotNull(reader.view.pageAt(400f, 300f)).second }
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_MOVE, finger(250f, 410f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
            val stroke = reader.strokes().single()
            assertEquals(AnnotationKind.HIGHLIGHTER, stroke.kind)
            assertEquals(listOf(start, end), stroke.points)
            val bitmap = main { Bitmap.createBitmap(reader.view.width, reader.view.height,
                Bitmap.Config.ARGB_8888).also { reader.view.draw(Canvas(it)) } }
            try {
                val inside = bitmap.getPixel(250, 300)
                assertEquals(255, android.graphics.Color.red(inside))
                assertTrue(android.graphics.Color.green(inside) > 220)
                assertTrue(android.graphics.Color.blue(inside) in 160..210)
                assertEquals("El desvío del dedo no deja tinta", android.graphics.Color.WHITE, bitmap.getPixel(250, 410))
            } finally { bitmap.recycle() }
        }
    }

    @Test fun highlighterAndPenAreIndependentAndStylusCancelsProvisionalPalmHighlight() {
        Reader().use { reader ->
            reader.enable(); click("Fluorescente")
            await { reader.view.penEnabled && reader.view.highlighterEnabled }
            reader.send(MotionEvent.ACTION_DOWN, finger(400f, 450f))
            reader.send(MotionEvent.ACTION_MOVE, finger(450f, 450f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                finger(450f, 450f), stylus(100f, 150f))
            reader.send(MotionEvent.ACTION_MOVE, stylus(150f, 170f), finger(450f, 450f))
            reader.send(MotionEvent.ACTION_POINTER_UP, stylus(200f, 150f), finger(450f, 450f))
            reader.send(MotionEvent.ACTION_UP, finger(500f, 450f))
            assertEquals(AnnotationKind.PEN, reader.strokes().single().kind)
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
            assertEquals(listOf(AnnotationKind.PEN, AnnotationKind.HIGHLIGHTER), reader.strokes().map { it.kind })
        }
    }

    @Test fun secondFingerCancelsHighlightAndZoomDoesNotLeaveAFingerTailMark() {
        Reader().use { reader ->
            click("Fluorescente"); await { reader.view.highlighterEnabled }
            val center = main { reader.view.width * 0.5f }
            val span = main { maxOf(ViewConfiguration.get(reader.view.context).scaledMinimumScalingSpan * 1.1f,
                reader.view.width * 0.35f) }
            fun left(factor: Float) = finger(center - span * factor / 2f, 300f)
            fun right(factor: Float) = finger(center + span * factor / 2f, 300f, 5)
            reader.send(MotionEvent.ACTION_DOWN, left(1f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.1f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(1f), right(1f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.3f), right(1.3f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.6f), right(1.6f))
            reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(1.6f), right(1.6f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.4f))
            reader.send(MotionEvent.ACTION_UP, left(1.4f))
            assertTrue(reader.strokes().isEmpty())
            main {
                val page = requireNotNull(reader.view.pageAt(center, 300f)).first
                assertTrue(page.scale > reader.view.width / page.size.width)
            }
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
            assertEquals(1, reader.strokes().size)
        }
    }

    @Test fun highlighterCancellationAndToolHidingNeverCommitProvisionalInk() {
        Reader().use { reader ->
            click("Fluorescente"); await { reader.view.highlighterEnabled }
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_MOVE, finger(400f, 300f))
            reader.send(MotionEvent.ACTION_CANCEL, finger(400f, 300f))
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f), flags = MotionEvent.FLAG_CANCELED)
            reader.send(MotionEvent.ACTION_DOWN, finger(100f, 300f))
            click("Ocultar barra"); await { !reader.view.highlighterEnabled }
            reader.send(MotionEvent.ACTION_UP, finger(400f, 300f))
            assertTrue(reader.strokes().isEmpty())
        }
    }

    @Test fun textSelectionCopiesActualPdfTextWithoutCreatingInkAndClearsOnModeChange() {
        Reader().use { reader ->
            reader.enable(); click("Fluorescente")
            click("Seleccionar texto")
            await { reader.view.textSelectionEnabled && !reader.view.penEnabled && !reader.view.highlighterEnabled }
            val page = main { requireNotNull(reader.view.pageAt(200f, 200f)).first }
            val a = page.toScreen(PagePoint(32f, 55f))
            val b = page.toScreen(PagePoint(168f, 55f))
            reader.send(MotionEvent.ACTION_DOWN, finger(a.x, a.y))
            reader.send(MotionEvent.ACTION_MOVE, finger(b.x, b.y))
            reader.send(MotionEvent.ACTION_UP, finger(b.x, b.y))
            await { reader.view.textSelection?.text == "Original text preserved." }
            assertTrue(main { reader.view.textSelection!!.quads.isNotEmpty() })
            click("Copiar")
            assertEquals("Original text preserved.", main { reader.activity.getSystemService(ClipboardManager::class.java)
                .primaryClip?.getItemAt(0)?.text?.toString() })
            click("Quitar selección")
            await { reader.view.textSelection == null }
            reader.send(MotionEvent.ACTION_DOWN, stylus(a.x, a.y))
            reader.send(MotionEvent.ACTION_UP, stylus(a.x, a.y))
            await { reader.view.textSelection?.text == "Original" }
            click("Bolígrafo")
            await { !reader.view.textSelectionEnabled && reader.view.textSelection == null && reader.view.penEnabled }
            assertTrue(reader.strokes().isEmpty())
            assertFalse(main { reader.model.document!!.pen.hasStrokes })
        }
    }

    @Test fun canceledOrInterruptedTextSelectionCannotReappearFromAsynchronousWork() {
        Reader().use { reader ->
            click("Seleccionar texto"); await { reader.view.textSelectionEnabled }
            val page = main { requireNotNull(reader.view.pageAt(200f, 200f)).first }
            val a = page.toScreen(PagePoint(32f, 55f))
            val b = page.toScreen(PagePoint(168f, 55f))
            reader.send(MotionEvent.ACTION_DOWN, finger(a.x, a.y))
            reader.send(MotionEvent.ACTION_MOVE, finger(b.x, b.y))
            reader.send(MotionEvent.ACTION_CANCEL, finger(b.x, b.y))
            click("Selección activa")
            await { !reader.view.textSelectionEnabled && reader.view.textSelection == null }
            SystemClock.sleep(300)
            assertNull(main { reader.view.textSelection })
            assertTrue(reader.strokes().isEmpty())
        }
    }

    @Test fun selectionRemainsAlignedDuringTwoFingerZoomAndPanWithoutCreatingInk() {
        Reader().use { reader ->
            click("Seleccionar texto"); await { reader.view.textSelectionEnabled }
            val page = main { requireNotNull(reader.view.pageAt(200f, 200f)).first }
            val a = page.toScreen(PagePoint(32f, 55f))
            reader.send(MotionEvent.ACTION_DOWN, finger(a.x, a.y))
            reader.send(MotionEvent.ACTION_UP, finger(a.x, a.y))
            await { reader.view.textSelection?.text == "Original" }
            val originalQuads = main { reader.view.textSelection!!.quads }
            val center = main { reader.view.width * 0.5f }
            val span = main { maxOf(ViewConfiguration.get(reader.view.context).scaledMinimumScalingSpan * 1.1f,
                reader.view.width * 0.35f) }
            fun left(factor: Float, y: Float = 350f) = finger(center - span * factor / 2f, y)
            fun right(factor: Float, y: Float = 350f) = finger(center + span * factor / 2f, y, 5)
            // Begin away from the text; the second finger cancels that provisional new selection.
            reader.send(MotionEvent.ACTION_DOWN, left(1f))
            reader.send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(1f), right(1f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.3f), right(1.3f))
            reader.send(MotionEvent.ACTION_MOVE, left(1.6f), right(1.6f))
            val beforePan = main { requireNotNull(reader.view.pageAt(center, 300f)).first }
            reader.send(MotionEvent.ACTION_MOVE, left(1.6f, 270f), right(1.6f, 270f))
            val afterPan = main { requireNotNull(reader.view.pageAt(center, 300f)).first }
            assertTrue(afterPan.scale > page.scale)
            assertTrue("Dos dedos también desplazan", afterPan.top < beforePan.top - 30f)
            reader.send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), left(1.6f, 270f), right(1.6f, 270f))
            reader.send(MotionEvent.ACTION_UP, left(1.6f, 270f))
            assertEquals(originalQuads, main { reader.view.textSelection!!.quads })
            val newA = afterPan.toScreen(PagePoint(32f, 55f))
            reader.send(MotionEvent.ACTION_DOWN, stylus(newA.x, newA.y))
            reader.send(MotionEvent.ACTION_UP, stylus(newA.x, newA.y))
            await { reader.view.textSelection?.text == "Original" }
            assertEquals(originalQuads, main { reader.view.textSelection!!.quads })
            assertTrue(reader.strokes().isEmpty())
        }
    }

    @Test fun hidingToolbarAndLeavingNeverSilentlyDiscardsCompletedInk() {
        Reader().use { reader ->
            reader.enable(); reader.write()
            click("Ocultar barra")
            await { !reader.view.penEnabled }
            assertEquals(1, reader.strokes().size)
            click("Mostrar barra")
            click("Atrás")
            click("Cancelar")
            assertNotNull(main { reader.model.document })
            assertFalse(main { reader.view.penEnabled })
            click("Atrás")
            val oldView = reader.view
            click("No guardar")
            await { reader.model.document == null }
            main { reader.model.openPdf(Uri.fromFile(reader.source), "Prueba.pdf") }
            await { reader.model.document != null && !reader.model.opening &&
                findReader(reader.activity.window.decorView)?.let { it !== oldView && it.isDocumentReady } == true }
            assertFalse(main { reader.view.penEnabled })
            assertTrue(reader.strokes().isEmpty())
        }
    }
}
