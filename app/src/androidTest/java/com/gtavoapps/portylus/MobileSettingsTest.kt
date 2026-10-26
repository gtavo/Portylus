package com.gtavoapps.portylus

import android.app.LocaleManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Looper
import android.os.LocaleList
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.preferences.ReaderPreferences
import com.gtavoapps.portylus.ui.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Actual Compose UI, Android locale recreation and native finger events; no network required. */
@RunWith(AndroidJUnit4::class)
class MobileSettingsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
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
            SystemClock.sleep(30)
        }
        error("Timed out waiting for app state")
    }
    private fun node(label: String, root: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == label || root.contentDescription?.toString() == label)) return root
        for (index in 0 until root.childCount) node(label, root.getChild(index))?.let { return it }
        return null
    }
    private fun visible(label: String): AccessibilityNodeInfo {
        instrumentation.waitForIdleSync()
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.uiAutomation.clearCache()
            node(label)?.let { return it }
            SystemClock.sleep(30)
        }
        error("Not visible: $label")
    }
    private fun click(label: String) {
        var target: AccessibilityNodeInfo? = visible(label)
        while (target != null && !target.isClickable) target = target.parent
        assertTrue("Could not click $label", target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        instrumentation.waitForIdleSync()
    }
    private fun reader(view: View): PdfReaderView? {
        if (view is PdfReaderView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) reader(view.getChildAt(index))?.let { return it }
        return null
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(180)
        val configuration = instrumentation.targetContext.resources.configuration
        val width = configuration.screenWidthDp
        val scale = configuration.fontScale
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "mobile-settings-$width-$scale-$name.png")
            .outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun languagesSettingsAndFingerInkSurviveRecreationWithoutSavingSource() {
        val context = instrumentation.targetContext
        val manager = context.getSystemService(LocaleManager::class.java)
        val previousLocales = manager.applicationLocales
        val preferences = ReaderPreferences(context)
        val previousFinger = preferences.fingerWriting
        val previousAwake = preferences.keepAwake
        main { manager.applicationLocales = LocaleList.forLanguageTags("es") }
        var activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = File.createTempFile("mobile-settings-", ".pdf", context.filesDir).apply {
            instrumentation.context.assets.open("base.pdf").use { input -> outputStream().use(input::copyTo) }
        }
        val original = source.readBytes()
        fun changeLanguage(label: String) {
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            try {
                click(label)
                activity = requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                await { activity.resources.configuration.locales[0].language == manager.applicationLocales[0].language }
            } finally { instrumentation.removeMonitor(monitor) }
        }
        try {
            await { !model.loading && model.stylesReady }
            visible("Ajustes"); visible("Abrir PDF"); assertNull(node("Apoyar en Ko-fi"))
            screenshot("home-es")
            main { model.openPdf(Uri.fromFile(source), "Documento con un nombre muy largo para comprobar la barra móvil.pdf") }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            screenshot("reader-es")
            // Primary tools are on-screen simultaneously and below the document controls on phones.
            val save = Rect().also { visible("Guardar").getBoundsInScreen(it) }
            for (label in listOf("Bolígrafo", "Fluorescente", "Seleccionar texto", "Goma", "Ocultar")) {
                val bounds = Rect().also { visible(label).getBoundsInScreen(it) }
                assertTrue(label, bounds.width() >= 47 * context.resources.displayMetrics.density)
                if (context.resources.configuration.screenWidthDp < 600) assertTrue(label, bounds.top >= save.bottom)
            }
            click("Más opciones"); click("Ajustes")
            screenshot("settings-es")
            if (!main { model.fingerWriting }) click("Escribir con el dedo")
            if (!main { model.keepAwake }) click("Mantener pantalla encendida")
            assertTrue(main { model.fingerWriting && model.keepAwake })
            assertTrue(ReaderPreferences(context).fingerWriting && ReaderPreferences(context).keepAwake)
            click("Cerrar")
            await { reader(activity.window.decorView)?.fingerWriting == true }
            click("Bolígrafo")
            await { reader(activity.window.decorView)?.penEnabled == true }
            val view = main { requireNotNull(reader(activity.window.decorView)) }
            val down = SystemClock.uptimeMillis()
            fun send(action: Int, points: List<Pair<Float, Float>>, tick: Long) {
                val properties = points.mapIndexed { index, _ -> MotionEvent.PointerProperties().apply {
                    id = index; toolType = MotionEvent.TOOL_TYPE_FINGER
                } }.toTypedArray()
                val coords = points.map { (a,b) -> MotionEvent.PointerCoords().apply {
                    x = a; y = b; pressure = 0.6f; size = 0.1f
                } }.toTypedArray()
                val event = MotionEvent.obtain(down, down + tick, action, points.size, properties, coords,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
                try { main { view.dispatchTouchEvent(event) } } finally { event.recycle() }
            }
            val x = main { view.width * 0.3f }; val y = main { view.height * 0.3f }
            send(MotionEvent.ACTION_DOWN, listOf(x to y), 0)
            send(MotionEvent.ACTION_MOVE, listOf(x + 30 to y + 20), 16)
            send(MotionEvent.ACTION_UP, listOf(x + 60 to y), 32)
            assertEquals(1, main { model.document!!.pen.strokes(0).size })
            // A second finger cancels provisional ink; lifting it must not create a tail stroke.
            send(MotionEvent.ACTION_DOWN, listOf(x to y + 70), 48)
            send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(x to y + 70, x + 100 to y + 70), 64)
            send(MotionEvent.ACTION_MOVE, listOf(x - 30 to y + 50, x + 140 to y + 50), 80)
            send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(x - 30 to y + 50, x + 140 to y + 50), 96)
            send(MotionEvent.ACTION_UP, listOf(x - 10 to y + 50), 112)
            assertEquals(1, main { model.document!!.pen.strokes(0).size })
            screenshot("finger-ink-es")
            val document = main { model.document }
            click("Más opciones"); click("Ajustes"); changeLanguage("English")
            visible("Settings"); screenshot("settings-en")
            assertSame(document, main { ViewModelProvider(activity)[AppViewModel::class.java].document })
            assertTrue(main { model.document!!.pen.isDirty })
            click("Close")
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            visible("Pen"); visible("Save"); screenshot("reader-en")
            click("More options"); click("Settings"); changeLanguage("Français")
            visible("Réglages"); screenshot("settings-fr")
            click("Fermer")
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            visible("Stylo"); visible("Enregistrer"); screenshot("reader-fr")
            assertSame(document, main { model.document })
            assertTrue(main { model.document!!.pen.isDirty })
            assertArrayEquals(original, source.readBytes())
            // Disable finger input through the French UI and verify persistence.
            click("Plus d’options"); click("Réglages"); click("Écrire avec le doigt")
            assertFalse(main { model.fingerWriting })
            assertFalse(ReaderPreferences(context).fingerWriting)
            click("Fermer")
            await { reader(activity.window.decorView)?.fingerWriting == false }
        } finally {
            main {
                model.updateFingerWriting(previousFinger); model.updateKeepAwake(previousAwake)
                model.closePdf(); activity.finish()
                manager.applicationLocales = previousLocales
            }
            source.delete()
            instrumentation.waitForIdleSync()
        }
    }
}
