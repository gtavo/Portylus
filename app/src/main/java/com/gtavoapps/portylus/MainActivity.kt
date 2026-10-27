package com.gtavoapps.portylus

import com.gtavoapps.portylus.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import com.gtavoapps.portylus.ui.PortylusTheme
import com.gtavoapps.portylus.ui.PortylusBrand
import com.gtavoapps.portylus.ui.SettingsDialog
import com.gtavoapps.portylus.ui.LegalDialog
import com.gtavoapps.portylus.ui.SearchBar
import com.gtavoapps.portylus.ui.ToolbarVisibilityAction
import androidx.compose.foundation.layout.imePadding
import com.gtavoapps.portylus.ui.ReaderToolbar
import com.gtavoapps.portylus.ui.ToolbarAction
import com.gtavoapps.portylus.ui.StyleBadge
import com.gtavoapps.portylus.ui.ActionIcon
import com.gtavoapps.portylus.ui.AfterSave
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gtavoapps.portylus.pdf.PdfReaderView
import com.gtavoapps.portylus.ui.BrowserScreen
import com.gtavoapps.portylus.ui.AppViewModel
import com.gtavoapps.portylus.ui.InkStyleDialog
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.EraserOptions
import com.gtavoapps.portylus.ui.EraserDialog

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val barColor = android.graphics.Color.rgb(7, 26, 51)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(barColor),
            navigationBarStyle = SystemBarStyle.dark(barColor))
        window.isNavigationBarContrastEnforced = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
        if (savedInstanceState == null) model.receivePdf(intent)
        setContent { PortylusTheme { PdfApp(model) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.receivePdf(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PdfApp(model: AppViewModel = viewModel()) {
    val strings = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var legalVisible by rememberSaveable { mutableStateOf(false) }
    if (settingsVisible) SettingsDialog(model,
        onInformation = { settingsVisible = false; legalVisible = true },
        onDismiss = { settingsVisible = false })
    if (legalVisible) LegalDialog { legalVisible = false }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.let { data ->
            data.data?.let { model.authorize(it, data.flags) }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.let { data ->
            data.data?.let { uri -> model.retainDocumentPermission(uri, data.flags); model.openPdf(uri) }
        }
    }
    var exportingRecovery by rememberSaveable { mutableStateOf(false) }
    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.let { data ->
            data.data?.let { uri ->
                model.retainDocumentPermission(uri, data.flags)
                if (exportingRecovery) model.exportRecovery(uri) else model.savePdf(uri, model.saveAfter)
            }
        }
        exportingRecovery = false
    }
    fun saveAs(after: AfterSave = AfterSave.STAY, recovery: Boolean = false) {
        exportingRecovery = recovery
        model.prepareSaveAs(after)
        savePicker.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, if (recovery) "Portylus-recuperado.pdf" else model.document?.name ?: "Documento.pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        })
    }
    LaunchedEffect(model.message) { model.message?.let { snackbar.showSnackbar(it); model.clearMessage() } }
    val document = model.document
    var toolbarVisible by rememberSaveable(document?.file?.name) { mutableStateOf(true) }
    var page by rememberSaveable(document?.file?.name) { mutableIntStateOf(document?.lastPage ?: 1) }
    // Intentionally not saveable: a recreated screen and every new document start in reading mode.
    var penEnabled by remember(document?.file?.name) { mutableStateOf(false) }
    var highlighterEnabled by remember(document?.file?.name) { mutableStateOf(false) }
    var textSelectionEnabled by remember(document?.file?.name) { mutableStateOf(false) }
    var eraserEnabled by remember(document?.file?.name) { mutableStateOf(false) }
    var eraserOptions by remember { mutableStateOf(EraserOptions()) }
    var showEraserOptions by remember { mutableStateOf(false) }
    var selectedText by remember(document?.file?.name) { mutableStateOf("") }
    var reader by remember(document?.file?.name) { mutableStateOf<PdfReaderView?>(null) }
    val context = LocalContext.current
    var closeRequest by remember(document?.file?.name) { mutableStateOf<AfterSave?>(null) }
    var moreActions by remember(document?.file?.name) { mutableStateOf(false) }
    var stylesFor by remember(document?.file?.name) { mutableStateOf<AnnotationKind?>(null) }
    fun requestClose() {
        if (model.saving) return
        reader?.cancelInteraction()
        penEnabled = false
        highlighterEnabled = false
        textSelectionEnabled = false
        eraserEnabled = false
        if (document?.pen?.isDirty == true) closeRequest = AfterSave.CLOSE else model.closePdf()
    }
    LaunchedEffect(model.fingerWriting) {
        reader?.cancelInteraction()
        penEnabled = false; highlighterEnabled = false; textSelectionEnabled = false; eraserEnabled = false
    }
    val currentView = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(currentView, document != null, model.keepAwake) {
        currentView.keepScreenOn = document != null && model.keepAwake
        onDispose { currentView.keepScreenOn = false }
    }
    LaunchedEffect(model.pendingExternalUri, model.opening, model.saving, document) {
        if (model.pendingExternalUri != null && !model.opening && !model.saving && document?.pen?.isDirty != true)
            model.openPendingPdf()
    }
    fun openSearch() {
        if (document == null || model.saving) return
        reader?.cancelInteraction()
        penEnabled = false; highlighterEnabled = false; textSelectionEnabled = false; eraserEnabled = false
        moreActions = false; toolbarVisible = true
        model.search.open(document.session)
    }
    BackHandler(enabled = document != null || model.opening || model.location != null) {
        if (model.search.state.open) model.search.close() else if (model.saving) Unit else if (model.opening) model.cancelOpening() else if (document != null) requestClose() else model.up()
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {

            if (document == null) {
                val compact = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width <
                    with(androidx.compose.ui.platform.LocalDensity.current) { 600.dp.toPx() }
                TopAppBar(title = { PortylusBrand(compact) }, actions = {
                    ToolbarAction(strings.getString(R.string.settings), onClick = { settingsVisible = true }) {
                        ActionIcon("settings")
                    }
                    ToolbarAction(strings.getString(R.string.open_pdf), enabled = !model.loading && !model.opening, onClick = {
                        pdfPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        })
                    }) { ActionIcon("open") }
                }, expandedHeight = 56.dp,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
            }
            else if (model.search.state.open) SearchBar(model.search)
            else if (toolbarVisible) ReaderToolbar(document.name, model.saving, ::requestClose,
                tools = { compact ->
                    ToolbarAction(if (penEnabled) strings.getString(R.string.pen_active) else strings.getString(R.string.pen), selected = penEnabled,
                        enabled = model.stylesReady && !model.saving && document.session.canAnnotate, onClick = {
                            eraserEnabled = false; textSelectionEnabled = false; penEnabled = !penEnabled
                            if (model.fingerWriting) highlighterEnabled = false
                        }) { ActionIcon("pen", tint = androidx.compose.material3.LocalContentColor.current) }
                    if (!compact) ToolbarAction(strings.getString(R.string.pen_styles), enabled = model.stylesReady && !model.saving,
                        onClick = { stylesFor = AnnotationKind.PEN }) {
                        StyleBadge(model.inkStyles.pen.current, model.inkStyles.pen.selected)
                    }
                    ToolbarAction(if (highlighterEnabled) strings.getString(R.string.highlight_active) else strings.getString(R.string.highlight), selected = highlighterEnabled,
                        enabled = model.stylesReady && !model.saving && document.session.canAnnotate, onClick = {
                            eraserEnabled = false; textSelectionEnabled = false; highlighterEnabled = !highlighterEnabled
                            if (model.fingerWriting) penEnabled = false
                        }) { ActionIcon("highlighter", tint = androidx.compose.material3.LocalContentColor.current) }
                    if (!compact) ToolbarAction(strings.getString(R.string.highlight_styles), enabled = model.stylesReady && !model.saving,
                        onClick = { stylesFor = AnnotationKind.HIGHLIGHTER }) {
                        StyleBadge(model.inkStyles.highlighter.current, model.inkStyles.highlighter.selected)
                    }
                    ToolbarAction(if (textSelectionEnabled) strings.getString(R.string.selection_active) else strings.getString(R.string.select_text), selected = textSelectionEnabled,
                        enabled = !model.saving, onClick = {
                            eraserEnabled = false; penEnabled = false; highlighterEnabled = false; textSelectionEnabled = !textSelectionEnabled
                        }) { ActionIcon("selectText", tint = androidx.compose.material3.LocalContentColor.current) }
                    ToolbarAction(if (eraserEnabled) strings.getString(R.string.eraser_active) else strings.getString(R.string.eraser), selected = eraserEnabled,
                        enabled = !model.saving && document.session.canAnnotate, onClick = {
                            penEnabled = false; highlighterEnabled = false; textSelectionEnabled = false; eraserEnabled = !eraserEnabled
                        }) { ActionIcon("eraser", tint = androidx.compose.material3.LocalContentColor.current) }
                    if (compact && !eraserEnabled) ToolbarAction(
                        strings.getString(if (highlighterEnabled) R.string.highlight_styles else R.string.pen_styles),
                        enabled = model.stylesReady && !model.saving,
                        onClick = { stylesFor = if (highlighterEnabled) AnnotationKind.HIGHLIGHTER else AnnotationKind.PEN }) {
                        val presets = if (highlighterEnabled) model.inkStyles.highlighter else model.inkStyles.pen
                        StyleBadge(presets.current, presets.selected)
                    }
                    if (eraserEnabled) ToolbarAction(strings.getString(R.string.eraser_options), enabled = !model.saving,
                        onClick = { showEraserOptions = true }) { ActionIcon("settings") }
                    ToolbarVisibilityAction(enabled = !model.saving, onClick = {
                        eraserEnabled = false; penEnabled = false; highlighterEnabled = false; textSelectionEnabled = false; toolbarVisible = false
                    })
                }, actions = { compact ->
                    ToolbarAction(strings.getString(R.string.search_text), enabled = !model.saving,
                        onClick = ::openSearch) { ActionIcon("search") }
                    ToolbarAction(strings.getString(R.string.undo), enabled = document.pen.canUndo && !model.saving, onClick = {
                        reader?.cancelInteraction(); document.pen.undo()
                    }) { ActionIcon("undo", document.pen.canUndo && !model.saving) }
                    ToolbarAction(strings.getString(R.string.redo), enabled = document.pen.canRedo && !model.saving, onClick = {
                        reader?.cancelInteraction(); document.pen.redo()
                    }) { ActionIcon("redo", document.pen.canRedo && !model.saving) }
                    if (compact) ToolbarAction(if (model.saving) strings.getString(R.string.saving) else strings.getString(R.string.save),
                        enabled = document.pen.isDirty && !model.saving, onClick = {
                            reader?.cancelInteraction(); model.savePdf()
                        }) { ActionIcon("save", document.pen.isDirty && !model.saving, MaterialTheme.colorScheme.primary) }
                    else Button(enabled = document.pen.isDirty && !model.saving, onClick = {
                        reader?.cancelInteraction(); model.savePdf()
                    }, modifier = Modifier.padding(horizontal = 4.dp)) { Text(if (model.saving) strings.getString(R.string.saving) else strings.getString(R.string.save)) }
                    Box {
                        ToolbarAction(strings.getString(R.string.more), enabled = !model.saving, onClick = { moreActions = true }) { ActionIcon("more") }
                        DropdownMenu(expanded = moreActions, onDismissRequest = { moreActions = false }) {
                            DropdownMenuItem(text = { Text(strings.getString(R.string.search_text)) },
                                leadingIcon = { ActionIcon("search") }, onClick = ::openSearch)
                            DropdownMenuItem(text = { Text(strings.getString(R.string.settings)) },
                                onClick = { moreActions = false; reader?.cancelInteraction(); settingsVisible = true })
                            DropdownMenuItem(text = { Text(strings.getString(R.string.pen_styles)) },
                                onClick = { moreActions = false; stylesFor = AnnotationKind.PEN })
                            DropdownMenuItem(text = { Text(strings.getString(R.string.highlight_styles)) },
                                onClick = { moreActions = false; stylesFor = AnnotationKind.HIGHLIGHTER })
                            DropdownMenuItem(text = { Text(strings.getString(R.string.privacy_licenses)) }, onClick = { moreActions = false; legalVisible = true })
                            DropdownMenuItem(text = { Text(strings.getString(R.string.save_as)) }, onClick = { moreActions = false; reader?.cancelInteraction(); saveAs() })
                            if (model.recoveryFile != null) DropdownMenuItem(text = { Text(strings.getString(R.string.recover_original_copy)) },
                                onClick = { moreActions = false; saveAs(recovery = true) })
                        }
                    }
                })
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).then(if (model.search.state.open) Modifier.imePadding() else Modifier)) {
            if (document == null) BrowserScreen(model) {
                treePicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                })
            } else {
                key(document.file.name) {
                    AndroidView(factory = { context -> PdfReaderView(context, document.session, document.pen, document.lastPage).apply {
                        reader = this
                        onPageChanged = { page = it; model.recordPage(document, it) }
                        onError = { error -> model.reportError(error) }
                        onTextSelectionChanged = { selectedText = it }
                    } }, update = {
                        val search = model.search.state
                        it.showSearch(search.matches, search.selected, search.navigation)
                        it.editingLocked = model.saving
                        it.annotationRevision = document.pen.revision
                        it.fingerWriting = model.fingerWriting
                        it.penEnabled = penEnabled
                        it.highlighterEnabled = highlighterEnabled
                        it.textSelectionEnabled = textSelectionEnabled
                        it.eraserOptions = eraserOptions
                        it.eraserEnabled = eraserEnabled
                        it.penStyle = model.inkStyles.pen.current
                        it.highlighterStyle = model.inkStyles.highlighter.current
                    }, modifier = Modifier.fillMaxSize())
                }
                if (textSelectionEnabled) Row(Modifier.align(Alignment.TopCenter).padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (selectedText.isEmpty()) Text(strings.getString(R.string.tap_text), Modifier.padding(12.dp))
                    else {
                        TextButton(onClick = {
                            context.getSystemService(ClipboardManager::class.java)
                                .setPrimaryClip(ClipData.newPlainText(strings.getString(R.string.pdf_text), selectedText))
                        }) { Text(strings.getString(R.string.copy)) }
                        TextButton(onClick = { reader?.clearTextSelection() }) { Text(strings.getString(R.string.clear_selection)) }
                    }
                }
                Text("$page / ${document.session.pageCount}", Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    .background(androidx.compose.ui.graphics.Color(0xe6071a33), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
                if (document.pen.isDirty) Text(strings.getString(R.string.unsaved), Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .background(androidx.compose.ui.graphics.Color(0xe6071a33), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
                if (!toolbarVisible) Box(Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(24.dp))) {
                    TextButton(onClick = { toolbarVisible = true }, modifier = Modifier.semantics {
                        contentDescription = strings.getString(R.string.show_toolbar)
                    }) {
                        ActionIcon("show")
                        Text(strings.getString(R.string.show_toolbar), Modifier.padding(start = 6.dp))
                    }
                }
            }
            if (model.opening || model.saving) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
    if (showEraserOptions && document != null) EraserDialog(eraserOptions,
        onChange = { eraserOptions = it }, onDismiss = { showEraserOptions = false })
    stylesFor?.let { kind ->
        InkStyleDialog(kind, model.inkStyles.presets(kind), model.savingStyle,
            onDismiss = { stylesFor = null },
            onSave = { slot, style -> model.saveInkStyle(kind, slot, style) { stylesFor = null } })
    }

    val exitAction = if (model.pendingExternalUri != null && document?.pen?.isDirty == true) AfterSave.OPEN_PENDING else closeRequest
    if (exitAction != null && !model.saving && model.saveFailure == null) AlertDialog(
        onDismissRequest = { closeRequest = null; if (exitAction == AfterSave.OPEN_PENDING) model.dismissExternalPdf() },
        title = { Text(if (exitAction == AfterSave.OPEN_PENDING) strings.getString(R.string.save_before_open) else strings.getString(R.string.save_changes)) },
        text = { Text(strings.getString(R.string.unsaved_explanation)) },
        confirmButton = { TextButton(onClick = {
            closeRequest = null; reader?.cancelInteraction(); model.savePdf(after = exitAction)
        }) { Text(if (exitAction == AfterSave.OPEN_PENDING) strings.getString(R.string.save_open) else strings.getString(R.string.save_exit)) } },
        dismissButton = { Row {
            TextButton(onClick = {
                closeRequest = null
                if (exitAction == AfterSave.OPEN_PENDING) model.openPendingPdf() else model.closePdf()
            }) { Text(strings.getString(R.string.discard)) }
            TextButton(onClick = {
                closeRequest = null
                if (exitAction == AfterSave.OPEN_PENDING) model.dismissExternalPdf()
            }) { Text(strings.getString(R.string.cancel)) }
        } })
    model.saveFailure?.let { error ->
        AlertDialog(onDismissRequest = { model.dismissSaveFailure(); model.dismissExternalPdf() },
            title = { Text(strings.getString(R.string.save_pending)) }, text = { Text(error) },
            confirmButton = { if (document != null) TextButton(onClick = {
                model.dismissSaveFailure(); saveAs(model.saveAfter)
            }) { Text(strings.getString(R.string.save_as)) } },
            dismissButton = { Row {
                if (model.recoveryFile != null) TextButton(onClick = { saveAs(recovery = true) }) { Text(strings.getString(R.string.recover_original)) }
                TextButton(onClick = { model.dismissSaveFailure(); model.dismissExternalPdf() }) { Text(strings.getString(R.string.continue_action)) }
            } })
    }

}

