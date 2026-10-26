package com.gtavoapps.portylus

import android.app.LocaleManager
import android.os.LocaleList
import androidx.test.runner.AndroidJUnitRunner

/** Stable Spanish fixture labels on any emulator; locale tests still switch all three languages. */
class PortylusTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        targetContext.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("es")
        super.onStart()
    }
}
