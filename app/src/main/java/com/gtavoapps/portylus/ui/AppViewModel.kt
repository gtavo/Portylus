package com.gtavoapps.portylus.ui

import com.gtavoapps.portylus.R
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gtavoapps.portylus.explorer.BrowserPreferences
import com.gtavoapps.portylus.explorer.DocumentEntry
import com.gtavoapps.portylus.explorer.FolderLocation
import com.gtavoapps.portylus.explorer.FolderRef
import com.gtavoapps.portylus.explorer.SortOrder
import com.gtavoapps.portylus.explorer.sortedEntries
import com.gtavoapps.portylus.explorer.withoutRoot
import com.gtavoapps.portylus.preferences.ReadingPositions
import com.gtavoapps.portylus.pdf.PdfSession
import com.gtavoapps.portylus.annotations.PenSession
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.InkPreferences
import com.gtavoapps.portylus.annotations.InkStyle
import com.gtavoapps.portylus.preferences.InkStylePreferences
import com.gtavoapps.portylus.preferences.FolderPreferences
import com.gtavoapps.portylus.storage.DocumentStorage
import com.gtavoapps.portylus.storage.VerifiedDocumentWrite
import com.gtavoapps.portylus.storage.DocumentWriteFailure
import com.gtavoapps.portylus.annotations.Annotation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class OpenPdf(var name: String, val file: File, val session: PdfSession, val pen: PenSession = PenSession(),
    var positionKey: String = "", var lastPage: Int = 1, var sourceUri: Uri? = null, var sourceHash: String = "")

enum class AfterSave { STAY, CLOSE, OPEN_PENDING }

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val search = DocumentSearch(viewModelScope)
    private val strings = application
    private val readerPreferences = com.gtavoapps.portylus.preferences.ReaderPreferences(application)
    var fingerWriting by mutableStateOf(readerPreferences.fingerWriting); private set
    var keepAwake by mutableStateOf(readerPreferences.keepAwake); private set

    fun updateFingerWriting(value: Boolean) {
        readerPreferences.setFingerWriting(value)
        fingerWriting = value
    }

    fun updateKeepAwake(value: Boolean) {
        readerPreferences.setKeepAwake(value)
        keepAwake = value
    }
    private val resolver = application.contentResolver
    private val storage = DocumentStorage(resolver)
    private val preferences = FolderPreferences(application)
    private val positions = ReadingPositions(application)
    private val initialized = CompletableDeferred<Unit>()
    var saving by mutableStateOf(false); private set
    var saveFailure by mutableStateOf<String?>(null); private set
    var recoveryFile by mutableStateOf<File?>(null); private set
    var saveAfter = AfterSave.STAY; private set
    private val recoveryDirectory = File(application.filesDir, "save-recovery").apply { mkdirs() }
    var pendingExternalUri by mutableStateOf<Uri?>(null); private set
    private val preferenceLock = Mutex()
    private val inkPreferences = InkStylePreferences(application)
    var inkStyles by mutableStateOf(InkPreferences()); private set
    var stylesReady by mutableStateOf(false); private set
    var savingStyle by mutableStateOf(false); private set
    private var listingJob: Job? = null
    private var listingRevision = 0
    private var openJob: Job? = null
    var settings by mutableStateOf(BrowserPreferences()); private set
    var location by mutableStateOf<FolderLocation?>(null); private set
    var entries by mutableStateOf<List<DocumentEntry>>(emptyList()); private set
    var listingError by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(true); private set
    var opening by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var document by mutableStateOf<OpenPdf?>(null); private set
    var expandedFolders by mutableStateOf<Set<String>>(emptySet()); private set
    var treeChildren by mutableStateOf<Map<String, List<FolderLocation>>>(emptyMap()); private set
    var treeErrors by mutableStateOf<Set<String>>(emptySet()); private set
    var treeLoading by mutableStateOf<Set<String>>(emptySet()); private set
    private val treeJobs = mutableMapOf<String, Job>()

    fun treeKey(folder: FolderLocation) = folder.treeUri + "\n" + folder.current.uri

    fun toggleTree(folder: FolderLocation) {
        val key = treeKey(folder)
        if (key in expandedFolders) expandedFolders = expandedFolders - key
        else {
            expandedFolders = expandedFolders + key
            loadTree(folder)
        }
    }

    fun retryTree(folder: FolderLocation) { loadTree(folder, force = true) }

    private fun rememberTree(folder: FolderLocation, result: List<DocumentEntry>) {
        val key = treeKey(folder)
        treeChildren = treeChildren + (key to sortedEntries(result, SortOrder.NAME).filter { it.isDirectory }
            .map { folder.copy(trail = folder.trail + FolderRef(it.uri, it.name)) })
        treeErrors = treeErrors - key
    }

    private fun loadTree(folder: FolderLocation, force: Boolean = false) {
        if (settings.roots.none { it.treeUri == folder.treeUri }) return
        val key = treeKey(folder)
        if (key in treeLoading || (!force && key in treeChildren)) return
        treeLoading = treeLoading + key
        treeErrors = treeErrors - key
        treeJobs[key] = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { storage.list(folder) }
                ensureActive()
                if (settings.roots.any { it.treeUri == folder.treeUri }) rememberTree(folder, result)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                treeErrors = treeErrors + key
            } finally {
                treeLoading = treeLoading - key
                treeJobs.remove(key)
            }
        }
    }

    private fun revealTree(folder: FolderLocation) {
        val ancestors = folder.trail.indices.map { folder.copy(trail = folder.trail.take(it + 1)) }
        expandedFolders = expandedFolders + ancestors.map(::treeKey)
        // Load only the open path, never recursively scan the user's storage.
        ancestors.dropLast(1).forEach { loadTree(it) }
    }
    var browserIndex = 0
    var browserOffset = 0

    init {
        viewModelScope.launch {
            try { inkStyles = withContext(Dispatchers.IO) { inkPreferences.read() } }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                message = strings.getString(R.string.styles_load_error)
            } finally { stylesReady = true }
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    application.cacheDir.listFiles()?.filter {
                        it.name.startsWith("reading-") && it.extension == "pdf"
                    }?.forEach { it.delete() }
                }
                recoveryFile = withContext(Dispatchers.IO) {
                    recoveryDirectory.listFiles()?.filter { (it.name.startsWith("portylus-backup-") || it.name.startsWith("pdfiz-backup-")) && it.length() > 0 }
                        ?.maxByOrNull { it.lastModified() }
                }
                if (recoveryFile != null) saveFailure = strings.getString(R.string.recovery_available)
                settings = withContext(Dispatchers.IO) { preferences.read() }
                // A missing last location means the user chose the library, not the first grant.
                settings.lastLocation?.let(::loadFolder) ?: run { loading = false }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = strings.getString(R.string.preferences_load_error)
                loading = false
            } finally { initialized.complete(Unit) }
        }
    }

    fun saveInkStyle(kind: AnnotationKind, slot: Int, style: InkStyle, onSaved: () -> Unit) {
        if (!stylesReady || savingStyle) return
        savingStyle = true
        viewModelScope.launch {
            try {
                val updated = inkStyles.save(kind, slot, style)
                withContext(Dispatchers.IO) { inkPreferences.write(updated) }
                inkStyles = updated
                onSaved()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = strings.getString(R.string.style_save_error)
            } finally { savingStyle = false }
        }
    }

    fun authorize(tree: Uri, grantedFlags: Int) {
        if (loading || opening) return
        viewModelScope.launch {
            try {
                val flags = grantedFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                require(flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
                resolver.takePersistableUriPermission(tree, flags)
                val root = withContext(Dispatchers.IO) { storage.root(tree) }
                updateSettings { it.copy(roots = it.roots.filterNot { item -> item.treeUri == root.treeUri } + root) }
                navigate(root)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = strings.getString(R.string.folder_permission_error)
            }
        }
    }

    fun navigate(target: FolderLocation) {
        if (opening) return
        loadFolder(target)
    }

    private fun loadFolder(target: FolderLocation) {
        listingJob?.cancel()
        val revision = ++listingRevision
        location = target
        revealTree(target)
        listingError = null
        entries = emptyList()
        browserIndex = 0
        browserOffset = 0
        loading = true
        listingJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { storage.list(target) }
                ensureActive()
                rememberTree(target, result)
                entries = sortedEntries(result, settings.sort)
                updateSettings { it.copy(lastLocation = target) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                listingError = strings.getString(R.string.folder_access_error)
            } finally { if (listingRevision == revision) loading = false }
        }
    }

    fun enter(entry: DocumentEntry) {
        val parent = location ?: return
        navigate(parent.copy(trail = parent.trail + FolderRef(entry.uri, entry.name)))
    }
    fun showHome() {
        if (opening) return
        listingJob?.cancel()
        ++listingRevision
        location = null
        entries = emptyList()
        listingError = null
        loading = false
        browserIndex = 0
        browserOffset = 0
        viewModelScope.launch { updateSettings { it.copy(lastLocation = null) } }
    }
    fun refreshFolder() { location?.let(::navigate) }
    fun up() {
        val current = location ?: return
        current.parent()?.let(::navigate) ?: showHome()
    }
    fun setSort(order: SortOrder) {
        viewModelScope.launch {
            updateSettings { it.copy(sort = order) }
            entries = sortedEntries(entries, settings.sort)
        }
    }
    fun toggleBookmark() {
        val current = location ?: return
        viewModelScope.launch { updateSettings {
            val exists = it.bookmarks.any { item -> item.current.uri == current.current.uri }
            it.copy(bookmarks = if (exists) it.bookmarks.filterNot { item -> item.current.uri == current.current.uri }
                else it.bookmarks + current)
        } }
    }
    fun moveRoot(index: Int, delta: Int) {
        viewModelScope.launch { updateSettings {
            val items = it.roots.toMutableList()
            if (index in items.indices && index + delta in items.indices) {
                val item = items.removeAt(index)
                items.add(index + delta, item)
            }
            it.copy(roots = items)
        } }
    }

    fun moveBookmark(index: Int, delta: Int) {
        viewModelScope.launch { updateSettings {
            val items = it.bookmarks.toMutableList()
            if (index in items.indices && index + delta in items.indices) {
                val item = items.removeAt(index)
                items.add(index + delta, item)
            }
            it.copy(bookmarks = items)
        } }
    }
    fun removeBookmark(index: Int) {
        viewModelScope.launch { updateSettings { it.copy(bookmarks = it.bookmarks.filterIndexed { i, _ -> i != index }) } }
    }

    fun removeRoot(treeUri: String) {
        if (opening || loading) return
        viewModelScope.launch {
            // Cancel an old listing before it can save the forgotten location again.
            listingJob?.cancel()
            ++listingRevision
            val removed = updateSettings { it.withoutRoot(treeUri) }
            if (!removed) return@launch
            val prefix = treeUri + "\n"
            treeJobs.filterKeys { it.startsWith(prefix) }.values.toList().forEach { it.cancel() }
            treeChildren = treeChildren.filterKeys { !it.startsWith(prefix) }
            expandedFolders = expandedFolders.filterNot { it.startsWith(prefix) }.toSet()
            treeErrors = treeErrors.filterNot { it.startsWith(prefix) }.toSet()
            val grant = resolver.persistedUriPermissions.firstOrNull { it.uri.toString() == treeUri }
            if (grant != null) runCatching {
                resolver.releasePersistableUriPermission(grant.uri,
                    (if (grant.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                        (if (grant.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0))
            }
            if (location?.treeUri == treeUri) {
                location = null; entries = emptyList(); listingError = null
                browserIndex = 0; browserOffset = 0; loading = false
                // Removing a grant leaves the user at the library, never in an unrelated folder.
            }
        }
    }

    fun receivePdf(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        if (uri.scheme != "content" && uri.scheme != "file") {
            reportError(strings.getString(R.string.pdf_address_error)); return
        }
        // Many senders grant only temporary access. Copy immediately; never require a durable grant.
        if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0 &&
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) runCatching {
            val grants = if (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            else Intent.FLAG_GRANT_READ_URI_PERMISSION
            resolver.takePersistableUriPermission(uri, grants)
        }
        pendingExternalUri = uri
    }

    fun dismissExternalPdf() { pendingExternalUri = null }
    fun openPendingPdf() {
        if (opening || saving) return
        val uri = pendingExternalUri ?: return
        pendingExternalUri = null
        closePdf()
        openPdf(uri)
    }

    fun recordPage(opened: OpenPdf, page: Int) {
        if (document !== opened || page !in 1..opened.session.pageCount) return
        opened.lastPage = page
        positions.write(opened.positionKey, page)
    }

    fun openPdf(uri: Uri, displayName: String? = null) {
        if (opening || saving || document != null) return
        opening = true
        openJob = viewModelScope.launch {
            var opened: OpenPdf? = null
            try {
                initialized.await()
                opened = withContext(Dispatchers.IO) {
                    val file = File.createTempFile("reading-", ".pdf", getApplication<Application>().cacheDir)
                    try {
                        storage.copyForReading(uri, file)
                        val name = displayName ?: runCatching { storage.displayName(uri) }.getOrNull() ?: "PDF"
                        val key = positions.key(uri)
                        val session = PdfSession.open(file)
                        try {
                            val ink = if (session.canAnnotate) (0 until session.pageCount).flatMap { page ->
                                session.annotations(page).filter { it.kind != AnnotationKind.TEXT }
                            } else emptyList()
                            if (ink.isNotEmpty()) session.replaceInk(emptyList())
                            OpenPdf(name, file, session, PenSession(ink), positionKey = key,
                                lastPage = positions.read(key, session.pageCount), sourceUri = uri,
                                sourceHash = VerifiedDocumentWrite.hash(file)).also { opened = it }
                        } catch (error: Throwable) { session.close(); throw error }
                    } catch (error: Throwable) { file.delete(); throw error }
                }
                ensureActive()
                document = opened
            } catch (error: Exception) {
                opened?.let { closeDocument(it) }
                if (error is CancellationException) throw error
                message = error.message?.let(strings::localizedError) ?: strings.getString(R.string.pdf_open_error)
            } finally { opening = false }
        }
    }
    fun closePdf() {
        if (saving) return
        val opened = document ?: return
        search.close()
        document = null
        viewModelScope.launch(Dispatchers.IO + NonCancellable) { closeDocument(opened) }
    }

    fun retainDocumentPermission(uri: Uri, flags: Int) {
        if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) runCatching {
            val grants = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (grants != 0) resolver.takePersistableUriPermission(uri, grants)
        }
    }

    fun prepareSaveAs(after: AfterSave) { saveAfter = after }
    fun dismissSaveFailure() { saveFailure = null }

    /** The only annotation-save entry point; invoked by explicit user actions only. */
    fun savePdf(destination: Uri? = null, after: AfterSave = AfterSave.STAY) {
        val opened = document ?: return
        if (saving) return
        val target = destination ?: opened.sourceUri ?: return
        saveAfter = after
        saving = true
        saveFailure = null
        val strokes = opened.pen.snapshot()
        viewModelScope.launch {
            var prepared: File? = null
            try {
                val writtenHash = withContext(Dispatchers.IO + NonCancellable) {
                    val output = File.createTempFile("portylus-prepared-", ".pdf", getApplication<Application>().cacheDir)
                    output.delete()
                    prepared = output
                    PdfSession.open(opened.file).use { native ->
                        if (native.canAnnotate) native.replaceInk(strokes)
                        else check(strokes.isEmpty()) { strings.getString(R.string.no_annotations) }
                        native.exportTo(output)
                    }
                    PdfSession.open(output).use { verified ->
                        check(verified.pageCount == opened.session.pageCount)
                        val actual = (0 until verified.pageCount).flatMap { verified.annotations(it) }
                            .filter { it.kind != AnnotationKind.TEXT }
                        if (opened.session.canAnnotate) check(actual.size == strokes.size)
                        for (stroke in strokes) {
                            val saved = actual.single { it.id == stroke.id }
                            check(saved.page == stroke.page && saved.kind == stroke.kind && saved.color == stroke.color)
                            check(kotlin.math.abs(saved.width - stroke.width) < 0.02f && saved.points.size == stroke.points.size)
                            check(saved.points.zip(stroke.points).all { (a, b) ->
                                kotlin.math.abs(a.x - b.x) < 0.02f && kotlin.math.abs(a.y - b.y) < 0.02f
                            })
                        }
                    }
                    storage.writeVerified(target, output,
                        opened.sourceHash.takeIf { positions.key(target) == positions.key(requireNotNull(opened.sourceUri)) },
                        recoveryDirectory)
                }
                opened.sourceUri = target
                opened.sourceHash = writtenHash
                opened.positionKey = positions.key(target)
                opened.name = withContext(Dispatchers.IO) {
                    runCatching { storage.displayName(target) }.getOrNull() ?: opened.name
                }
                positions.write(opened.positionKey, opened.lastPage)
                opened.pen.markSaved(strokes)
                saving = false
                message = strings.getString(R.string.pdf_saved)
                when (after) {
                    AfterSave.STAY -> Unit
                    AfterSave.CLOSE -> closePdf()
                    AfterSave.OPEN_PENDING -> openPendingPdf()
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                recoveryFile = (error as? DocumentWriteFailure)?.recovery ?: recoveryFile
                saveFailure = if (error is DocumentWriteFailure) error.message?.let(strings::localizedError)
                    else strings.getString(R.string.save_error)
            } finally {
                prepared?.delete()
                saving = false
            }
        }
    }

    fun exportRecovery(destination: Uri) {
        val original = recoveryFile ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO + NonCancellable) {
                    storage.writeVerified(destination, original, null, recoveryDirectory)
                }
                original.delete()
                recoveryFile = null
                saveFailure = null
                message = strings.getString(R.string.copy_recovered)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                saveFailure = strings.getString(R.string.recovery_error)
            } finally { saving = false }
        }
    }

    fun cancelOpening() { openJob?.cancel() }
    fun clearMessage() { message = null }
    fun reportError(error: String) { message = error }

    private suspend fun updateSettings(change: (BrowserPreferences) -> BrowserPreferences): Boolean =
        preferenceLock.withLock {
            try {
                val updated = change(settings)
                withContext(Dispatchers.IO) { preferences.write(updated) }
                settings = updated
                true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = strings.getString(R.string.settings_save_error)
                false
            }
        }
    private fun closeDocument(opened: OpenPdf) {
        try { opened.session.close() } finally { opened.file.delete() }
    }
    override fun onCleared() {
        search.close()
        openJob?.cancel()
        document?.let(::closeDocument)
    }
}
