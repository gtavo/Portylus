package com.gtavoapps.portylus



import android.content.Intent

import android.graphics.Bitmap

import android.net.Uri

import android.os.Looper

import android.os.SystemClock

import android.provider.DocumentsContract

import android.view.accessibility.AccessibilityNodeInfo

import androidx.lifecycle.ViewModelProvider

import androidx.test.ext.junit.runners.AndroidJUnit4

import androidx.test.platform.app.InstrumentationRegistry

import com.gtavoapps.portylus.explorer.*

import com.gtavoapps.portylus.preferences.FolderPreferences

import com.gtavoapps.portylus.ui.AppViewModel

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*

import org.junit.Test

import org.junit.runner.RunWith

import java.io.File

import java.util.concurrent.atomic.AtomicReference



@RunWith(AndroidJUnit4::class)

class BrowserWorkflowTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun <T> main(block: () -> T): T {

        if (Looper.myLooper() == Looper.getMainLooper()) return block()

        val value = AtomicReference<Result<T>>()

        instrumentation.runOnMainSync { value.set(runCatching(block)) }

        return value.get().getOrThrow()

    }

    private fun await(condition: () -> Boolean) {

        val limit = SystemClock.uptimeMillis() + 15000

        while (SystemClock.uptimeMillis() < limit) {

            if (main(condition)) return

            SystemClock.sleep(30)

        }

        error("El explorador no alcanzó el estado esperado")

    }

    private fun click(label: String) {
        // Recycled Compose rows must publish their new semantics before accessibility targets them.
        instrumentation.waitForIdleSync()

        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {

            if (node == null) return null

            if ((node.text?.toString() == label || node.contentDescription?.toString() == label) &&

                node.isVisibleToUser && node.isEnabled) {

                var target: AccessibilityNodeInfo? = node

                while (target != null && !target.isClickable) target = target.parent

                if (target?.isEnabled == true) return target

            }

            for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }

            return null

        }

        val limit = SystemClock.uptimeMillis() + 10000

        while (SystemClock.uptimeMillis() < limit) {

            instrumentation.uiAutomation.clearCache()

            val target = find(instrumentation.uiAutomation.rootInActiveWindow)

            if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return

            SystemClock.sleep(40)

        }

        error("No se puede pulsar: $label")

    }

    private fun screenshot(name: String) {

        instrumentation.waitForIdleSync()

        SystemClock.sleep(200)

        val suffix = if (instrumentation.targetContext.resources.configuration.screenWidthDp < 600) "-movil" else ""

        val image = instrumentation.uiAutomation.takeScreenshot()

        File(instrumentation.targetContext.getExternalFilesDir(null), "Portylus-v0.0.9-$name$suffix.png")

            .outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }

        image.recycle()

    }

    private fun tree(id: String) = Uri.parse("content://com.gtavoapps.portylus.test.folders/tree/$id")

    private fun ref(root: String, id: String, name: String) =

        FolderRef(DocumentsContract.buildDocumentUriUsingTree(tree(root),id).toString(),name)

    private val root get() = FolderLocation(tree("root").toString(),listOf(ref("root","root","Matemáticas")))

    private val archive get() = FolderLocation(tree("archive").toString(),listOf(ref("archive","archive","Archivo")))

    private val course get() = root.copy(trail = root.trail + ref("root","course","1º ESO"))



    private inner class Browser(last: FolderLocation? = null) : AutoCloseable {

        val preferences = FolderPreferences(instrumentation.targetContext)

        val previous = runBlocking { preferences.read() }

        val activity: MainActivity

        val model: AppViewModel

        init {

            instrumentation.targetContext.startActivity(Intent().setClassName(

                "com.gtavoapps.portylus.test","com.gtavoapps.portylus.FixtureFolderGrantActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

            await { runCatching {

                instrumentation.targetContext.contentResolver.query(Uri.parse(root.current.uri),null,null,null,null)!!

                    .use { it.moveToFirst() }

            }.getOrDefault(false) }

            runBlocking { preferences.write(BrowserPreferences(listOf(root,archive),listOf(course,archive),last)) }

            activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,MainActivity::class.java)

                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity

            model = main { ViewModelProvider(activity)[AppViewModel::class.java] }

            await { !model.loading }

        }

        fun at(id: String) = await { !model.loading && model.location?.current?.uri == ref("root",id,"").uri }

        override fun close() {

            main { model.closePdf(); activity.finish() }

            instrumentation.waitForIdleSync()

            runBlocking { preferences.write(previous) }

        }

    }



    private fun openNavigation() {

        if (instrumentation.targetContext.resources.configuration.screenWidthDp < 800) click("Mostrar carpetas")

    }



    @Test fun treeAndUnifiedListFollowTheSameFolderWithMetadataAndPdfBack() {

        Browser().use { browser ->

            assertNull(main { browser.model.location })

            openNavigation()

            screenshot("inicio")

            click("Seleccionar carpeta Matemáticas"); browser.at("root")

            screenshot("carpeta")

            assertEquals(listOf("1º ESO","2º ESO","Aula.pdf","Zeta.pdf"),main { browser.model.entries.map { it.name } })

            assertTrue(main { browser.model.entries.filterNot { it.isDirectory }.all { it.sizeBytes == 1399L && it.modified > 0 } })

            click("Ordenar archivos"); click("Más recientes")

            await { browser.model.settings.sort == SortOrder.MODIFIED }

            assertEquals(listOf("1º ESO","2º ESO","Zeta.pdf","Aula.pdf"),main { browser.model.entries.map { it.name } })

            click("Abrir carpeta 1º ESO"); browser.at("course")

            click("Abrir carpeta Geometría"); browser.at("geometry")

            await { browser.model.treeChildren[browser.model.treeKey(course)]?.any { it.current.name == "Geometría" } == true }

            assertTrue(main {

                listOf(root,course).all { browser.model.treeKey(it) in browser.model.expandedFolders }

            })

            openNavigation()

            screenshot("ruta")

            click("Seleccionar carpeta Matemáticas"); browser.at("root")

            click("Abrir PDF Aula.pdf")
            val states = mutableListOf<String>()
            try {
                await {
                    val state = "opening=" + browser.model.opening + ", name=" + browser.model.document?.name +
                        ", message=" + browser.model.message + ", folder=" + browser.model.location?.current?.name
                    if (states.lastOrNull() != state) states.add(state)
                    browser.model.document?.name == "Aula.pdf" && !browser.model.opening
                }
            } catch (error: Exception) {
                screenshot("apertura-fallida")
                throw AssertionError(states.joinToString("\n"), error)
            }

            click("Atrás"); await { browser.model.document == null }

            assertEquals("Matemáticas",main { browser.model.location!!.current.name })

            main { browser.activity.onBackPressedDispatcher.onBackPressed() }

            await { browser.model.location == null && browser.model.settings.lastLocation == null }

            assertNull(runBlocking { browser.preferences.read() }.lastLocation)

            assertFalse(browser.activity.isFinishing)

        }

    }



    @Test fun editReordersBothListsAndRemovesOnlyReferencesWhileBookmarksRevealTheTree() {

        Browser().use { browser ->

            openNavigation()

            click("Abrir bookmark 1º ESO"); browser.at("course")

            await { browser.model.treeChildren[browser.model.treeKey(root)] != null }

            assertTrue(main { browser.model.treeKey(root) in browser.model.expandedFolders })

            openNavigation()

            click("Editar")

            click("Editar carpeta Archivo"); click("Subir")

            await { browser.model.settings.roots.first() == archive }

            click("Editar bookmark Archivo"); click("Subir")

            await { browser.model.settings.bookmarks.first() == archive }

            screenshot("edicion")

            assertEquals(listOf(archive,root),runBlocking { browser.preferences.read() }.roots)

            assertEquals(listOf(archive,course),runBlocking { browser.preferences.read() }.bookmarks)

            click("Editar bookmark 1º ESO"); click("Quitar")

            await { browser.model.settings.bookmarks == listOf(archive) }

            click("Editar carpeta Matemáticas"); click("Quitar"); click("Cancelar")

            assertEquals(2,main { browser.model.settings.roots.size })

            val pdf = DocumentsContract.buildDocumentUriUsingTree(tree("root"),"aula")

            val resolver = instrumentation.targetContext.contentResolver

            val original = resolver.openInputStream(pdf)!!.use { it.readBytes() }

            click("Editar carpeta Matemáticas"); click("Quitar"); click("Quitar carpeta")

            await { browser.model.settings.roots == listOf(archive) && browser.model.location == null }

            assertEquals(listOf(archive),runBlocking { browser.preferences.read() }.roots)

            assertTrue(main { browser.model.treeChildren.keys.none { it.startsWith(root.treeUri + "\\n") } })

            assertArrayEquals(original,resolver.openInputStream(pdf)!!.use { it.readBytes() })

            assertTrue(resolver.query(Uri.parse(root.current.uri),null,null,null,null)!!.use { it.moveToFirst() })

        }

    }



    @Test fun lastFolderRestoresItsExpandedPathAndUnavailableFolderOffersWayBack() {

        Browser(course).use { browser ->

            browser.at("course")

            await { browser.model.treeChildren[browser.model.treeKey(root)] != null }

            assertTrue(main { browser.model.treeKey(root) in browser.model.expandedFolders })

            val absent = root.copy(trail = root.trail + ref("root","missing","No disponible"))

            main { browser.model.navigate(absent) }

            await { !browser.model.loading && browser.model.listingError != null }

            screenshot("sin-acceso")

            click("Volver a mis carpetas")

            await { browser.model.location == null && browser.model.settings.lastLocation == null }

            assertFalse(browser.activity.isFinishing)

        }

    }

}

