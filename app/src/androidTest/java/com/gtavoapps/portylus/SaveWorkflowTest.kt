package com.gtavoapps.portylus

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.PdfSession
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.ui.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SaveWorkflowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun <T> main(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
    private fun await(condition: () -> Boolean) {
        val until = SystemClock.uptimeMillis() + 20000
        while (SystemClock.uptimeMillis() < until) {
            if (main(condition)) return
            SystemClock.sleep(25)
        }
        error("Portylus no alcanzó el estado esperado del guardado.")
    }
    private fun findReader(view: View): PdfReaderView? {
        if (view is PdfReaderView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findReader(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun click(label: String) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if ((node.text?.toString() == label || node.contentDescription?.toString() == label) && node.isVisibleToUser && node.isEnabled) return node
            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
            return null
        }
        val until = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < until) {
            instrumentation.uiAutomation.clearCache()
            var node=find(instrumentation.uiAutomation.rootInActiveWindow)
            while (node != null && !node.isClickable) node=node.parent
            if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
            SystemClock.sleep(30)
        }
        error("No se encuentra: $label")
    }
    private inner class Reader : AutoCloseable {
        val source=File.createTempFile("save-test-", ".pdf", instrumentation.targetContext.filesDir).apply {
            instrumentation.context.assets.open("base.pdf").use { input -> outputStream().use(input::copyTo) }
        }
        val original=source.readBytes()
        val activity=instrumentation.startActivitySync(Intent(instrumentation.targetContext,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model=main { ViewModelProvider(activity)[AppViewModel::class.java] }
        init { await { !model.loading }; open(source) }
        fun open(file: File) {
            main { model.openPdf(Uri.fromFile(file),"Prueba.pdf") }
            await { model.document != null && !model.opening && findReader(activity.window.decorView)?.isDocumentReady == true }
        }
        fun add(page: Int = 0): Annotation {
            val stroke=Annotation(page=page,kind=AnnotationKind.PEN,points=listOf(PagePoint(40f,100f),PagePoint(120f,100f)),width=4f)
            main { model.document!!.pen.add(stroke) }
            return stroke
        }
        override fun close() {
            main { model.closePdf(); activity.finish() }
            instrumentation.waitForIdleSync()
            source.delete()
        }
    }
    @Test fun manualSaveUndoRedoAndReopenKeepNativeEditableInk() {
        Reader().use { reader ->
            val a=reader.add(); reader.add(1)
            assertArrayEquals(reader.original,reader.source.readBytes())
            click("Deshacer"); await { reader.model.document!!.pen.snapshot().size == 1 }
            click("Rehacer"); await { reader.model.document!!.pen.snapshot().size == 2 }
            assertArrayEquals(reader.original,reader.source.readBytes())
            click("Guardar"); await { !reader.model.saving && !reader.model.document!!.pen.isDirty }
            val saved=reader.source.readBytes()
            assertFalse(saved.contentEquals(reader.original))
            PdfSession.open(reader.source).use {
                assertEquals(listOf(a.id),it.annotations(0).map { mark -> mark.id })
                assertEquals(1,it.annotations(1).size)
            }
            reader.add(); click("Deshacer"); await { !reader.model.document!!.pen.isDirty }
            click("Deshacer"); await { reader.model.document!!.pen.isDirty }
            assertArrayEquals(saved,reader.source.readBytes())
            click("Rehacer"); await { !reader.model.document!!.pen.isDirty }
            click("Atrás"); await { reader.model.document == null }
            reader.open(reader.source)
            assertEquals(2,main { reader.model.document!!.pen.snapshot().size })
            assertFalse(main { reader.model.document!!.pen.isDirty || reader.model.document!!.pen.canUndo })
            assertTrue(main { reader.model.document!!.session.annotations(0).isEmpty() })
            // Saved ink remains a live overlay, and can be erased, undone and saved again without duplicates.
            main { reader.model.document!!.pen.replace(0,emptyList()) }
            click("Guardar"); await { !reader.model.saving && !reader.model.document!!.pen.isDirty }
            PdfSession.open(reader.source).use { assertTrue(it.annotations(0).isEmpty());assertEquals(1,it.annotations(1).size) }
            click("Deshacer"); await { reader.model.document!!.pen.isDirty }
            click("Guardar"); await { !reader.model.saving && !reader.model.document!!.pen.isDirty }
            PdfSession.open(reader.source).use { assertEquals(1,it.annotations(0).size);assertEquals(1,it.annotations(1).size) }
        }
    }
    @Test fun discardDoesNotWriteAnnotationsButAlwaysRemembersPage() {
        Reader().use { reader ->
            reader.add()
            main { reader.model.recordPage(reader.model.document!!,2) }
            click("Atrás"); click("No guardar"); await { reader.model.document == null }
            assertArrayEquals(reader.original,reader.source.readBytes())
            reader.open(reader.source)
            assertEquals(2,main { reader.model.document!!.lastPage })
            assertTrue(main { reader.model.document!!.pen.snapshot().isEmpty() })
            assertFalse(main { reader.model.document!!.pen.isDirty })
        }
    }
    @Test fun saveAndExitWaitsForVerifiedWrite() {
        Reader().use { reader ->
            reader.add(); click("Atrás"); click("Guardar y salir")
            await { reader.model.document == null && !reader.model.saving }
            assertNull(main { reader.model.saveFailure })
            PdfSession.open(reader.source).use { assertEquals(1,it.annotations(0).size) }
        }
    }
    @Test fun readOnlyProviderOffersSaveAsAndKeepsChangesAndOriginal() {
        Reader().use { reader ->
            val uri=Uri.parse("content://com.gtavoapps.portylus.test.fixture/sample.pdf")
            val original=instrumentation.targetContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            main { reader.model.closePdf(); reader.model.openPdf(uri) }
            await { reader.model.document != null && !reader.model.opening }
            reader.add()
            click("Guardar"); await { !reader.model.saving && reader.model.saveFailure != null }
            assertTrue(main { reader.model.document!!.pen.isDirty })
            assertArrayEquals(original,instrumentation.targetContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
            val destination=File.createTempFile("save-as-", ".pdf", instrumentation.targetContext.filesDir)
            try {
                main { reader.model.savePdf(Uri.fromFile(destination)) }
                await { !reader.model.saving && !reader.model.document!!.pen.isDirty }
                assertNull(main { reader.model.saveFailure })
                PdfSession.open(destination).use { assertEquals(1,it.annotations(0).size) }
                assertArrayEquals(original,instrumentation.targetContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() })
                assertEquals(Uri.fromFile(destination),main { reader.model.document!!.sourceUri })
            } finally { destination.delete() }
        }
    }
    @Test fun savedInkOnRotatedCroppedPageRendersAtTheSameCoordinates() {
        Reader().use { reader ->
            reader.add(1); click("Guardar"); await { !reader.model.saving && !reader.model.document!!.pen.isDirty }
            PdfSession.open(reader.source).use { native ->
                val size=native.pageSize(1)
                val bitmap=native.render(1,size.width.toInt())
                try {
                    val pixel=bitmap.getPixel((80f-size.originX).toInt(),(100f-size.originY).toInt())
                    assertEquals(0xff2255cc.toInt(),pixel)
                } finally { bitmap.recycle() }
            }
        }
    }
}
