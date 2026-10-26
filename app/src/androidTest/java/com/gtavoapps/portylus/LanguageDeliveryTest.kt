package com.gtavoapps.portylus

import android.app.LocaleManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/** Also runs against release APK splits: no debug-only model or reader APIs. */
@RunWith(AndroidJUnit4::class)
class LanguageDeliveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(LocaleManager::class.java)

    @Test fun installedResourcesIncludeEveryPickerLanguage() {
        for ((tag, expected) in listOf("es" to "Ajustes", "en" to "Settings", "fr" to "Réglages")) {
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            val localized = context.createConfigurationContext(configuration)
            val id = localized.resources.getIdentifier("settings", "string", context.packageName)
            assertEquals("Missing installed $tag resources (test the bundle, not just a universal APK)",
                expected, localized.getString(id))
        }
    }

    private fun node(label: String, root: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isVisibleToUser && (root.text?.toString() == label || root.contentDescription?.toString() == label)) return root
        for (index in 0 until root.childCount) node(label, root.getChild(index))?.let { return it }
        return null
    }
    private fun visible(label: String): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.clearCache()
            node(label)?.let { return it }
            SystemClock.sleep(30)
        }
        error("Not visible: $label")
    }
    private fun click(label: String) {
        var target: AccessibilityNodeInfo? = visible(label)
        while (target != null && !target.isClickable) target = target.parent
        assertTrue("Cannot click $label", target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        instrumentation.waitForIdleSync()
    }
    private fun scrollable(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isScrollable && root.isVisibleToUser) return root
        for (index in 0 until root.childCount) scrollable(root.getChild(index))?.let { return it }
        return null
    }
    private fun scrollTo(label: String) {
        repeat(8) {
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.clearCache()
            if (node(label) != null) return
            assertTrue(scrollable(instrumentation.uiAutomation.rootInActiveWindow)
                ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true)
            SystemClock.sleep(150)
        }
        visible(label)
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(180)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(context.getExternalFilesDir(null), "language-delivery-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun pickerPersistsAndSupportAppearsOnlyInSettings() {
        val previous = manager.applicationLocales
        instrumentation.runOnMainSync { manager.applicationLocales = LocaleList.forLanguageTags("es") }
        fun launch() = instrumentation.startActivitySync(Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        var activity = launch()
        fun change(label: String, tag: String, title: String) {
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            try {
                click(label)
                activity = requireNotNull(monitor.waitForActivityWithTimeout(15000)) as MainActivity
                assertEquals(tag, activity.resources.configuration.locales[0].language)
                visible(title)
                visible(activity.getString(R.string.language))
            } finally { instrumentation.removeMonitor(monitor) }
        }
        try {
            visible("Abrir PDF")
            assertNull(node("Apoyar en Ko-fi"))
            screenshot("home-es")
            click("Ajustes")
            change("English", "en", "Settings")
            screenshot("settings-en")
            click("Close")
            visible("Open PDF")
            assertNull(node("Support on Ko-fi"))
            instrumentation.runOnMainSync { activity.finish() }
            instrumentation.waitForIdleSync()
            activity = launch()
            visible("Open PDF")
            assertEquals("en", activity.resources.configuration.locales[0].language)
            click("Settings")
            change("Français", "fr", "Réglages")
            screenshot("settings-fr")
            click("Fermer")
            visible("Ouvrir un PDF")
            assertNull(node("Soutenir sur Ko-fi"))
            click("Réglages")
            change("Español", "es", "Ajustes")
            scrollTo("Apoyar en Ko-fi")
            visible("¿Te gusta Portylus? Apoya el proyecto.")
            screenshot("support-es")
            scrollTo("Privacidad y licencias")
            click("Privacidad y licencias")
            visible("Acerca de")
            assertNull(node("Apoyar en Ko-fi"))
            click("Cerrar")
            visible("Abrir PDF")
            assertNull(node("Apoyar en Ko-fi"))
            click("Ajustes")
            val systemLanguage = manager.systemLocales[0].language
            val systemTitle = when (systemLanguage) { "en" -> "Settings"; "fr" -> "Réglages"; else -> "Ajustes" }
            change("Idioma del sistema", systemLanguage, systemTitle)
            assertTrue(manager.applicationLocales.isEmpty)
        } finally {
            instrumentation.runOnMainSync { activity.finish(); manager.applicationLocales = previous }
            instrumentation.waitForIdleSync()
        }
    }
}
