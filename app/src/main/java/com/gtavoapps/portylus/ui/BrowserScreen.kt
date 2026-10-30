package com.gtavoapps.portylus.ui

import com.gtavoapps.portylus.R
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.explorer.DocumentEntry
import com.gtavoapps.portylus.explorer.FolderLocation
import com.gtavoapps.portylus.explorer.SortOrder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Keep the familiar Windows-style folder/bookmark accent within the shared light/dark palette.
private val ColorScheme.explorerAccent: Color
    get() = if (surface.luminance() > 0.5f) Color(0xFF885200) else Color(0xFFFFC857)

/** One folder tree, its bookmarks and one contents list, also on narrow screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BrowserScreen(model: AppViewModel, authorize: () -> Unit,
    openDownloads: () -> Unit, addDownloadsFolder: () -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    var downloadsHelp by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var removeRoot by remember { mutableStateOf<FolderLocation?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 800.dp
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val choose: (FolderLocation) -> Unit = {
            model.navigate(it)
            if (!wide) scope.launch { drawer.close() }
        }
        BackHandler(enabled = !wide && drawer.isOpen) { scope.launch { drawer.close() } }
        val navigation: @Composable () -> Unit = {
            FolderNavigation(model, editing, onEdit = { editing = !editing }, authorize,
                choose = choose, removeRoot = { removeRoot = it }, downloadsHelp = { downloadsHelp = true })
        }
        if (wide) Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(280.dp).fillMaxHeight()) { navigation() }
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Contents(model, Modifier.weight(1f), null)
        } else ModalNavigationDrawer(drawerState = drawer,
            gesturesEnabled = drawer.isOpen,
            drawerContent = {
                ModalDrawerSheet(Modifier.width(304.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) {
                    navigation()
                }
            }) {
            Contents(model, Modifier.fillMaxSize()) { scope.launch { drawer.open() } }
        }
    }
    if (downloadsHelp) AlertDialog(
        onDismissRequest = { downloadsHelp = false },
        title = { Text(strings.getString(R.string.downloads)) },
        text = { Text(strings.getString(R.string.downloads_explanation)) },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                TextButton(onClick = { downloadsHelp = false; addDownloadsFolder() }) {
                    Text(strings.getString(R.string.add_downloads_subfolder))
                }
                TextButton(onClick = { downloadsHelp = false; openDownloads() }) {
                    Text(strings.getString(R.string.open_downloads_pdf))
                }
                TextButton(onClick = { downloadsHelp = false }) { Text(strings.getString(R.string.cancel)) }
            }
        })
    removeRoot?.let { root ->
        AlertDialog(onDismissRequest = { removeRoot = null },
            title = { Text(strings.getString(R.string.remove_root_title, root.current.name)) },
            text = { Text(strings.getString(R.string.remove_root_body)) },
            confirmButton = { TextButton(onClick = { model.removeRoot(root.treeUri); removeRoot = null }) { Text(strings.getString(R.string.remove_folder)) } },
            dismissButton = { TextButton(onClick = { removeRoot = null }) { Text(strings.getString(R.string.cancel)) } })
    }
}

private data class TreeRow(val folder: FolderLocation, val depth: Int, val rootIndex: Int? = null)

@Composable private fun FolderNavigation(model: AppViewModel, editing: Boolean, onEdit: () -> Unit,
    authorize: () -> Unit, choose: (FolderLocation) -> Unit, removeRoot: (FolderLocation) -> Unit,
    downloadsHelp: () -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    val rows = buildList {
        fun branch(folder: FolderLocation, depth: Int, rootIndex: Int? = null) {
            add(TreeRow(folder, depth, rootIndex))
            if (model.treeKey(folder) in model.expandedFolders) {
                model.treeChildren[model.treeKey(folder)].orEmpty().forEach {
                    // A malformed provider must not create an infinite navigation tree.
                    if (folder.trail.none { ancestor -> ancestor.uri == it.current.uri }) branch(it, depth + 1)
                }
            }
        }
        model.settings.roots.forEachIndexed { index, root -> branch(root, 0, index) }
    }
    val scroll = rememberLazyListState()
    LaunchedEffect(model.location, rows.map { model.treeKey(it.folder) }) {
        val selected = rows.indexOfFirst {
            it.folder.treeUri == model.location?.treeUri && it.folder.current.uri == model.location?.current?.uri
        }
        if (selected >= 0 && scroll.layoutInfo.visibleItemsInfo.none { it.index == selected }) {
            scroll.animateScrollToItem(selected)
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(strings.getString(R.string.my_folders), Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onEdit, enabled = !model.opening && !model.loading) {
                Text(if (editing) strings.getString(R.string.done) else strings.getString(R.string.edit))
            }
        }
        LazyColumn(state = scroll, modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = { "tree:" + model.treeKey(it.folder) }) { row ->
                val folder = row.folder
                val key = model.treeKey(folder)
                val expanded = key in model.expandedFolders
                val children = model.treeChildren[key]
                val hasChildren = children == null || children.isNotEmpty()
                NavigationRow(folder, selected = model.location?.let {
                    it.treeUri == folder.treeUri && it.current.uri == folder.current.uri
                } == true, depth = row.depth,
                    description = strings.getString(R.string.select_folder_prefix) + folder.current.name,
                    onClick = { choose(folder) },
                    leading = {
                        if (hasChildren || key in model.treeErrors) IconButton(
                            onClick = { model.toggleTree(folder) }, modifier = Modifier.size(40.dp).semantics {
                                contentDescription = (if (expanded) strings.getString(R.string.collapse_prefix) else strings.getString(R.string.expand_prefix)) + folder.current.name
                            }) { ActionIcon(if (expanded) "down" else "chevron") }
                        else Spacer(Modifier.width(40.dp))
                    },
                    trailing = {
                        if (editing && row.rootIndex != null) EditMenu(
                            strings.getString(R.string.folder_prefix) + folder.current.name, row.rootIndex, model.settings.roots.lastIndex,
                            move = { model.moveRoot(row.rootIndex, it) }, remove = { removeRoot(folder) })
                    })
                if (expanded && key in model.treeLoading) Text(strings.getString(R.string.loading),
                    Modifier.padding(start = (48 + row.depth * 16).dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (expanded && key in model.treeErrors) TextButton(onClick = { model.retryTree(folder) },
                    modifier = Modifier.padding(start = (32 + row.depth * 16).dp)) {
                    Text(strings.getString(R.string.retry_access), style = MaterialTheme.typography.labelSmall)
                }
            }
            item(key = "add") {
                TextButton(enabled = !model.loading && !model.opening, onClick = authorize,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text(strings.getString(R.string.add_folder)) }
            }
            item(key = "downloads-help") {
                TextButton(enabled = !model.loading && !model.opening, onClick = downloadsHelp,
                    modifier = Modifier.fillMaxWidth()) { Text(strings.getString(R.string.downloads_help)) }
            }
            item(key = "bookmarks") {
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(start = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActionIcon("star", tint = MaterialTheme.colorScheme.explorerAccent)
                    Text(strings.getString(R.string.bookmarks), Modifier.padding(start = 10.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                if (model.settings.bookmarks.isEmpty()) Text(strings.getString(R.string.bookmark_hint),
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(model.settings.bookmarks, key = { "bookmark:" + model.treeKey(it) }) { folder ->
                val index = model.settings.bookmarks.indexOf(folder)
                NavigationRow(folder, selected = false, depth = 0,
                    description = strings.getString(R.string.open_bookmark_prefix) + folder.current.name,
                    detail = folder.trail.dropLast(1).joinToString(" › ") { it.name }.ifBlank { null },
                    onClick = { choose(folder) },
                    leading = { Spacer(Modifier.width(12.dp)) },
                    trailing = {
                        if (editing) EditMenu(strings.getString(R.string.bookmark_prefix) + folder.current.name, index,
                            model.settings.bookmarks.lastIndex, move = { model.moveBookmark(index, it) },
                            remove = { model.removeBookmark(index) })
                    })
            }
        }
    }
}

@Composable private fun NavigationRow(folder: FolderLocation, selected: Boolean, depth: Int,
    description: String, detail: String? = null, onClick: () -> Unit,
    leading: @Composable () -> Unit, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = (depth * 16).dp, bottom = 2.dp)
        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically) {
        leading()
        Row(Modifier.weight(1f).heightIn(min = 48.dp).clickable(onClick = onClick)
            .semantics { contentDescription = description }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            ActionIcon("folder", tint = MaterialTheme.colorScheme.explorerAccent)
            Column(Modifier.weight(1f).padding(start = 8.dp, end = 4.dp)) {
                Text(folder.current.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                if (detail != null) Text(detail, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable private fun EditMenu(label: String, index: Int, lastIndex: Int, move: (Int) -> Unit, remove: () -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(40.dp).semantics {
            contentDescription = strings.getString(R.string.edit_prefix) + label
        }) { ActionIcon("more") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(strings.getString(R.string.move_up)) }, enabled = index > 0,
                onClick = { expanded = false; move(-1) })
            DropdownMenuItem(text = { Text(strings.getString(R.string.move_down)) }, enabled = index < lastIndex,
                onClick = { expanded = false; move(1) })
            DropdownMenuItem(text = { Text(strings.getString(R.string.remove)) }, onClick = { expanded = false; remove() })
        }
    }
}

@Composable private fun Contents(model: AppViewModel, modifier: Modifier, showFolders: (() -> Unit)?) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    val location = model.location
    Column(modifier.fillMaxHeight().padding(horizontal = if (showFolders != null) 8.dp else 16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showFolders != null) TextButton(onClick = showFolders,
                modifier = Modifier.semantics { contentDescription = strings.getString(R.string.show_folders) }) {
                ActionIcon("folder"); Spacer(Modifier.width(6.dp)); Text(strings.getString(R.string.folders))
            }
            if (location != null) {
                IconButton(onClick = model::up, modifier = Modifier.semantics { contentDescription = strings.getString(R.string.parent_folder) }) {
                    ActionIcon("back")
                }
                Text(location.current.name, Modifier.weight(1f).semantics { heading() },
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val favorite = model.settings.bookmarks.any { it.current.uri == location.current.uri }
                IconButton(onClick = model::toggleBookmark, modifier = Modifier.semantics {
                    contentDescription = if (favorite) strings.getString(R.string.remove_bookmark) else strings.getString(R.string.add_bookmark)
                }) { ActionIcon(if (favorite) "starFilled" else "star", tint = MaterialTheme.colorScheme.explorerAccent) }
            } else Text(strings.getString(R.string.browser), Modifier.weight(1f).padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium)
        }
        if (location != null) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                location.trail.forEachIndexed { index, folder ->
                    if (index > 0) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { model.navigate(location.copy(trail = location.trail.take(index + 1))) },
                        modifier = Modifier.semantics { contentDescription = strings.getString(R.string.go_to_prefix) + folder.name }) {
                        Text(folder.name, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (model.loading) strings.getString(R.string.loading) else if (model.listingError != null) strings.getString(R.string.access_unavailable)
                    else model.entries.size.toString() + strings.getString(R.string.items_suffix),
                    Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SortMenu(model.settings.sort, onSort = model::setSort)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            model.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            location == null -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                ActionIcon("folder", tint = MaterialTheme.colorScheme.explorerAccent)
                Text(strings.getString(R.string.choose_folder), Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                Text(if (showFolders == null) strings.getString(R.string.folders_left)
                    else strings.getString(R.string.folders_drawer), Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            model.listingError != null -> Column(Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(strings.getString(R.string.folder_error), style = MaterialTheme.typography.titleMedium)
                Text(strings.getString(R.string.folder_error_hint), Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = model::refreshFolder) { Text(strings.getString(R.string.retry)) }
                TextButton(onClick = model::showHome) { Text(strings.getString(R.string.back_folders)) }
            }
            else -> key(location) { DetailsList(model) }
        }
    }
}

@Composable private fun DetailsList(model: AppViewModel) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    val list = rememberLazyListState(model.browserIndex, model.browserOffset)
    val location = model.location
    DisposableEffect(location) {
        onDispose {
            if (model.location == location) {
                model.browserIndex = list.firstVisibleItemIndex
                model.browserOffset = list.firstVisibleItemScrollOffset
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = maxWidth >= 510.dp
        Column {
            if (columns) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(strings.getString(R.string.name), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(strings.getString(R.string.size), Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium)
                Text(strings.getString(R.string.modified), Modifier.width(166.dp), style = MaterialTheme.typography.labelMedium)
            }
            if (model.entries.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.getString(R.string.empty_folder), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(model.entries, key = { it.uri }) { entry ->
                    EntryRow(entry, columns, !model.opening) {
                        if (entry.isDirectory) model.enter(entry)
                        else {
                            model.browserIndex = list.firstVisibleItemIndex
                            model.browserOffset = list.firstVisibleItemScrollOffset
                            model.openPdf(Uri.parse(entry.uri), entry.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun EntryRow(entry: DocumentEntry, columns: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    val size = entry.sizeBytes?.let {
        android.text.format.Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, it)
    } ?: "—"
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val modified = remember(entry.modified, locale) {
        if (entry.modified > 0) java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, locale).format(Date(entry.modified)) else "—"
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable(enabled = enabled, onClick = onClick)
        .semantics { contentDescription = (if (entry.isDirectory) strings.getString(R.string.open_folder_prefix) else strings.getString(R.string.open_pdf_prefix)) + entry.name }
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (entry.isDirectory) ActionIcon("folder", tint = MaterialTheme.colorScheme.explorerAccent)
        else Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
            Text("PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 12.dp)) {
            Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            if (!columns && !entry.isDirectory) Text("$size · $modified", Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (columns) {
            Text(if (entry.isDirectory) "" else size, Modifier.width(88.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (entry.isDirectory) "" else modified, Modifier.width(166.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun SortMenu(order: SortOrder, onSort: (SortOrder) -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.semantics { contentDescription = strings.getString(R.string.sort_files) }) {
            Text(if (order == SortOrder.NAME) strings.getString(R.string.sort_name) else strings.getString(R.string.sort_recent), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(6.dp)); ActionIcon("sort")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(strings.getString(R.string.sort_name)) }, onClick = { expanded = false; onSort(SortOrder.NAME) })
            DropdownMenuItem(text = { Text(strings.getString(R.string.sort_recent)) }, onClick = { expanded = false; onSort(SortOrder.MODIFIED) })
        }
    }
}
