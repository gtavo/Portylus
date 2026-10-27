package com.gtavoapps.portylus.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.gtavoapps.portylus.R

@Composable fun PageJumpDialog(currentPage: Int, totalPages: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = currentPage.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val target = input.text.trim().toIntOrNull()?.takeIf { it in 1..totalPages }
    val invalid = input.text.isNotEmpty() && target == null
    fun dismiss() { keyboard?.hide(); onDismiss() }
    fun go() { target?.let { keyboard?.hide(); onGo(it) } }
    AlertDialog(onDismissRequest = ::dismiss,
        title = { Text(strings.getString(R.string.go_to_page)) },
        text = {
            Column {
                OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    label = { Text(strings.getString(R.string.page_number)) }, isError = invalid,
                    supportingText = { Text(strings.getString(if (invalid) R.string.page_jump_invalid else R.string.page_range, totalPages)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { go() }))
            }
        },
        confirmButton = { TextButton(enabled = target != null, onClick = ::go) { Text(strings.getString(R.string.go)) } },
        dismissButton = { TextButton(onClick = ::dismiss) { Text(strings.getString(R.string.cancel)) } })
    LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
}
