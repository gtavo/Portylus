package com.gtavoapps.portylus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.R

/** Replaces the annotation toolbar while searching; it does not stack another toolbar above the PDF. */
@Composable fun SearchBar(search: DocumentSearch) {
    val state = search.state
    val strings = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    fun dismissKeyboard() { keyboard?.hide(); focus.clearFocus() }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = state.query, onValueChange = search::query, singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(requester)
                        .semantics { contentDescription = strings.getString(R.string.search_text) },
                    placeholder = { Text(strings.getString(R.string.search_placeholder)) },
                    leadingIcon = { ActionIcon("search") },
                    trailingIcon = { if (state.query.isNotEmpty()) ToolbarAction(strings.getString(R.string.clear_search),
                        onClick = { search.query("") }) { ActionIcon("close") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }))
                ToolbarAction(strings.getString(R.string.close_search), onClick = { dismissKeyboard(); search.close() }) { ActionIcon("close") }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).heightIn(min = 48.dp)
                    .toggleable(value = state.wholeWords, role = Role.Checkbox, onValueChange = search::wholeWords)
                    .semantics { contentDescription = strings.getString(R.string.whole_words) },
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.wholeWords, onCheckedChange = null)
                    Text(strings.getString(R.string.whole_words), style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, modifier = Modifier.weight(1f))
                }
                val current = if (state.selected < 0) 0 else state.selected + 1
                Text("$current/${state.matches.size}${if (state.truncated) "+" else ""}",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 4.dp)
                        .semantics { contentDescription = strings.getString(R.string.search_counter, current, state.matches.size) })
                ToolbarAction(strings.getString(R.string.previous_match), enabled = state.matches.isNotEmpty(),
                    onClick = { dismissKeyboard(); search.move(-1) }) { ActionIcon("previous") }
                ToolbarAction(strings.getString(R.string.next_match), enabled = state.matches.isNotEmpty(),
                    onClick = { dismissKeyboard(); search.move(1) }) { ActionIcon("next") }
            }
            val notice = when {
                state.failed -> strings.getString(R.string.search_error)
                state.query.isBlank() || state.busy -> null
                state.truncated -> strings.getString(R.string.search_limited)
                !state.hasText -> strings.getString(R.string.search_no_text)
                state.matches.isEmpty() -> strings.getString(R.string.search_no_matches)
                else -> null
            }
            if (notice != null) Text(notice, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp))
            if (state.busy) LinearProgressIndicator(progress = { state.searchedPages.toFloat() / state.totalPages.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(2.dp).semantics { contentDescription = strings.getString(R.string.searching) })
            else HorizontalDivider()
        }
    }
    LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
}
