package com.gtavoapps.portylus

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.ui.AppViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SearchWorkflowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun <T> main(block: () -> T): T {
        val result=AtomicReference<Result<T>>()
        instrumentation.runOnMainSync { result.set(runCatching(block)) };return result.get().getOrThrow()
    }
    private fun await(block: () -> Boolean) {
        val until=SystemClock.uptimeMillis()+15000
        while(SystemClock.uptimeMillis()<until) { if(main(block))return;SystemClock.sleep(30) }
        error("Search did not reach expected state")
    }
    private fun node(label: String, root: AccessibilityNodeInfo?=instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if(root==null)return null
        if(root.isVisibleToUser && (root.text?.toString()==label || root.contentDescription?.toString()==label))return root
        for(i in 0 until root.childCount)node(label,root.getChild(i))?.let{return it};return null
    }
    private fun visible(label: String): AccessibilityNodeInfo {
        val until=SystemClock.uptimeMillis()+10000
        while(SystemClock.uptimeMillis()<until) {
            instrumentation.waitForIdleSync();instrumentation.uiAutomation.clearCache()
            node(label)?.let{return it};SystemClock.sleep(30)
        };error("Not visible: $label")
    }
    private fun click(label: String) {
        var target:AccessibilityNodeInfo?=visible(label)
        while(target!=null&&!target.isClickable)target=target.parent
        assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        instrumentation.waitForIdleSync()
    }
    private fun type(query: String) {
        fun editable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if(root==null)return null
            if(root.isVisibleToUser && root.isEditable)return root
            for(i in 0 until root.childCount)editable(root.getChild(i))?.let{return it}
            return null
        }
        val args=Bundle().apply {putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,query)}
        val until=SystemClock.uptimeMillis()+10000
        while(SystemClock.uptimeMillis()<until) {
            instrumentation.waitForIdleSync();instrumentation.uiAutomation.clearCache()
            if(editable(instrumentation.uiAutomation.rootInActiveWindow)
                ?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args)==true)return
            SystemClock.sleep(30)
        }
        error("Search field is not editable")
    }
    private fun reader(view:View):PdfReaderView? {
        if(view is PdfReaderView)return view
        if(view is ViewGroup)for(i in 0 until view.childCount)reader(view.getChildAt(i))?.let{return it}
        return null
    }
    private fun screenshot(name:String) {
        instrumentation.waitForIdleSync();SystemClock.sleep(200)
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        val width=instrumentation.targetContext.resources.configuration.screenWidthDp
        File(instrumentation.targetContext.getExternalFilesDir(null),"search-$width-$name.png").outputStream().use{
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        };bitmap.recycle()
    }
    @Test fun searchFindsPartialAndWholeWordsNavigatesRecreatesAndNeverChangesPendingInkOrSource() {
        val context=instrumentation.targetContext
        var activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model=main{ViewModelProvider(activity)[AppViewModel::class.java]}
        val source=SearchFixture.create(context);val original=source.readBytes()
        fun searchDone(count:Int) {await{!model.search.state.busy && model.search.state.matches.size==count}}
        try {
            await{!model.loading&&model.stylesReady}
            main{model.openPdf(Uri.fromFile(source),"Prueba de búsqueda.pdf")}
            await{reader(activity.window.decorView)?.isDocumentReady==true}
            val document=main{requireNotNull(model.document)}
            main{document.pen.add(Annotation(page=0,kind=AnnotationKind.PEN,
                points=listOf(PagePoint(20f,40f),PagePoint(100f,50f))))}
            screenshot("toolbar")
            click("Más opciones");click("Buscar texto");type("pdf");searchDone(12)
            await{reader(activity.window.decorView)?.searchHighlightCount==12}
            click("Palabras completas");searchDone(4)
            repeat(3){click("Coincidencia siguiente")}
            await{reader(activity.window.decorView)?.activeSearchMatch?.page==1}
            assertEquals(3,main{model.search.state.selected})
            screenshot("whole-page2")
            click("Coincidencia siguiente");assertEquals(0,main{model.search.state.selected})
            click("Coincidencia anterior");assertEquals(3,main{model.search.state.selected})
            type("izador");searchDone(0);visible("Sin coincidencias")
            click("Palabras completas");searchDone(2)
            click("Coincidencia siguiente");screenshot("partial")
            val monitor=instrumentation.addMonitor(MainActivity::class.java.name,null,false)
            try {
                main{activity.recreate()};activity=requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                visible("Cerrar búsqueda")
                assertSame(model,main{ViewModelProvider(activity)[AppViewModel::class.java]})
                assertEquals("izador",main{model.search.state.query})
                assertEquals(1,main{model.search.state.selected})
                await{reader(activity.window.decorView)?.activeSearchMatch?.page==1}
            }finally{instrumentation.removeMonitor(monitor)}
            type("zzzzzz");type("café");searchDone(4)
            click("Borrar búsqueda");searchDone(0)
            await{reader(activity.window.decorView)?.searchHighlightCount==0}
            type("pdf");searchDone(12);click("Cerrar búsqueda")
            visible("Ocultar barra")
            await{reader(activity.window.decorView)?.searchHighlightCount==0}
            assertTrue(main{document.pen.isDirty})
            assertEquals(1,main{document.pen.strokes(0).size})
            assertFalse(main{reader(activity.window.decorView)!!.penEnabled})
            click("Ocultar barra");visible("Mostrar barra");screenshot("hidden-toolbar")
            click("Mostrar barra");visible("Ocultar barra")
            assertArrayEquals(original,source.readBytes())
        }finally{main{model.search.close();model.closePdf();activity.finish()};source.delete()}
    }
    @Test fun imageOnlyPdfExplainsWhyThereAreNoHitsAndClosingCancelsTheQuery() {
        val context=instrumentation.targetContext
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val model=main{ViewModelProvider(activity)[AppViewModel::class.java]}
        val source=SearchFixture.create(context,text=false)
        try{
            await{!model.loading};main{model.openPdf(Uri.fromFile(source))}
            await{reader(activity.window.decorView)?.isDocumentReady==true}
            click("Más opciones");click("Buscar texto");type("pdf")
            await{!model.search.state.busy && model.search.state.searchedPages==3}
            visible("Este PDF no tiene texto para buscar. Puede ser un escaneo.")
            type("otra");click("Cerrar búsqueda");SystemClock.sleep(350)
            assertFalse(main{model.search.state.open});assertTrue(main{model.search.state.matches.isEmpty()})
        }finally{main{model.search.close();model.closePdf();activity.finish()};source.delete()}
    }
}
