package com.gtavoapps.portylus

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.preferences.AppAppearance
import com.gtavoapps.portylus.preferences.ReaderPreferences
import com.gtavoapps.portylus.ui.AppViewModel
import com.gtavoapps.portylus.ui.PortylusDarkColors
import com.gtavoapps.portylus.ui.PortylusLightColors
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Real theme selection, persistence and AndroidView reuse with unsaved PDF ink. */
@RunWith(AndroidJUnit4::class)
class AppearanceWorkflowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun <T> main(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) }
        return result.get().getOrThrow()
    }
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (main(condition)) return
            SystemClock.sleep(40)
        }
        error("Appearance did not reach the expected state")
    }
    private fun find(label: String, root: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == label || root.contentDescription?.toString() == label)) return root
        for (i in 0 until root.childCount) find(label, root.getChild(i))?.let { return it }
        return null
    }
    private fun scroll(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        if (root.isScrollable && root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        for (i in 0 until root.childCount) if (scroll(root.getChild(i))) return true
        return false
    }
    private fun click(label: String) {
        repeat(12) {
            instrumentation.waitForIdleSync(); instrumentation.uiAutomation.clearCache()
            var target = find(label)
            if (target != null) {
                while (target != null && !target.isClickable) target = target.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    instrumentation.waitForIdleSync(); return
                }
            }
            scroll(instrumentation.uiAutomation.rootInActiveWindow)
            SystemClock.sleep(100)
        }
        error("Control not reachable: $label")
    }
    private fun reader(view: View): PdfReaderView? {
        if (view is PdfReaderView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) reader(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync(); SystemClock.sleep(400)
        val config = instrumentation.targetContext.resources.configuration
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null),
            "appearance-${config.screenWidthDp}-${config.fontScale}-$name.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
    @Test fun themeSelectionPreservesDocumentInkAndPageAndSurvivesRecreation() {
        val context = instrumentation.targetContext
        val previous = ReaderPreferences(context).appearance
        var activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model = main { ViewModelProvider(activity)[AppViewModel::class.java] }
        val source = File.createTempFile("appearance-", ".pdf", context.filesDir).apply {
            instrumentation.context.assets.open("base.pdf").use { input -> outputStream().use(input::copyTo) }
        }
        val original = source.readBytes()
        try {
            await { !model.loading && model.stylesReady }
            for (mode in listOf(AppAppearance.LIGHT, AppAppearance.DARK)) {
                main { model.updateAppearance(mode) }
                screenshot("explorer-${mode.preferenceValue}")
            }
            main { model.openPdf(Uri.fromFile(source), "Documento de prueba de apariencia.pdf") }
            await { reader(activity.window.decorView)?.isDocumentReady == true }
            val document = main { requireNotNull(model.document) }
            val view = main { requireNotNull(reader(activity.window.decorView)) }
            main { document.pen.add(Annotation(page = 0, kind = AnnotationKind.PEN,
                points = listOf(PagePoint(30f, 70f), PagePoint(120f, 80f)))) }
            val ink = main { document.pen.snapshot() }
            val page = main { document.lastPage }
            for ((mode, label) in listOf(AppAppearance.LIGHT to "Claro", AppAppearance.DARK to "Oscuro", AppAppearance.SYSTEM to "Seguir el sistema")) {
                click("Más opciones"); click("Ajustes"); click("Cambiar apariencia"); click(label)
                await { model.appearance == mode }
                assertEquals(mode, ReaderPreferences(context).appearance)
                screenshot("settings-${mode.preferenceValue}")
                click("Cerrar")
                val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                val dark = mode.usesDarkTheme(systemDark)
                val colors = if (dark) PortylusDarkColors else PortylusLightColors
                await { view.workspaceColor == colors.surfaceContainer.toArgb() }
                assertSame("Changing theme must not replace the native reader", view, main { reader(activity.window.decorView) })
                assertSame(document, main { model.document })
                assertEquals(page, main { document.lastPage })
                assertEquals(ink, main { document.pen.snapshot() })
                assertTrue(main { document.pen.isDirty })
                assertArrayEquals(original, source.readBytes())
                val bars = main { WindowInsetsControllerCompat(activity.window, activity.window.decorView) }
                assertEquals(!dark, main { bars.isAppearanceLightStatusBars })
                assertEquals(!dark, main { bars.isAppearanceLightNavigationBars })
                screenshot("reader-${mode.preferenceValue}")
            }
            main { model.updateAppearance(AppAppearance.LIGHT) }
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            try {
                main { activity.recreate() }
                activity = requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                await { reader(activity.window.decorView)?.isDocumentReady == true }
                assertSame(document, main { ViewModelProvider(activity)[AppViewModel::class.java].document })
                assertEquals(AppAppearance.LIGHT, main { model.appearance })
                assertEquals(AppAppearance.LIGHT, ReaderPreferences(context).appearance)
                assertEquals(PortylusLightColors.surfaceContainer.toArgb(), main { reader(activity.window.decorView)!!.workspaceColor })
                assertEquals(ink, main { document.pen.snapshot() })
                assertEquals(page, main { document.lastPage })
                assertArrayEquals(original, source.readBytes())
            } finally { instrumentation.removeMonitor(monitor) }
        } catch (error: Throwable) {
            screenshot("failure"); throw error
        } finally {
            main { model.updateAppearance(previous); model.closePdf(); activity.finish() }
            source.delete()
        }
    }
}
