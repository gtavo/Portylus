package com.gtavoapps.portylus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gtavoapps.portylus.pdf.PdfSession
import com.gtavoapps.portylus.pdf.SearchMatch
import kotlinx.coroutines.*

data class SearchState(val open: Boolean = false, val query: String = "", val wholeWords: Boolean = false,
    val matches: List<SearchMatch> = emptyList(), val selected: Int = -1, val busy: Boolean = false,
    val searchedPages: Int = 0, val totalPages: Int = 0, val hasText: Boolean = false,
    val failed: Boolean = false, val truncated: Boolean = false, val navigation: Long = 0)

/** Per-document state survives activity recreation. A cancelled query can never publish stale hits. */
class DocumentSearch(private val scope: CoroutineScope) {
    var state by mutableStateOf(SearchState()); private set
    private var session: PdfSession? = null
    private var job: Job? = null
    private var generation = 0L

    fun open(document: PdfSession) {
        if (session !== document) close()
        session = document
        state = state.copy(open = true, totalPages = document.pageCount)
    }
    fun close() {
        generation++; job?.cancel(); job = null; session = null
        state = SearchState(navigation = state.navigation + 1)
    }
    fun query(value: String) { update(value, state.wholeWords) }
    fun wholeWords(value: Boolean) { update(state.query, value) }
    private fun update(query: String, wholeWords: Boolean) {
        val document = session ?: return
        val token = ++generation
        job?.cancel()
        state = SearchState(open = true, query = query, wholeWords = wholeWords,
            totalPages = document.pageCount, busy = query.isNotBlank(), navigation = state.navigation + 1)
        if (query.isBlank()) return
        job = scope.launch {
            try {
                delay(250)
                val hits = ArrayList<SearchMatch>()
                var hasText = false
                for (page in 0 until document.pageCount) {
                    ensureActive()
                    val found = withContext(Dispatchers.IO) {
                        document.searchText(page, query, wholeWords, MAX_MATCHES - hits.size)
                    }
                    ensureActive()
                    if (token != generation || session !== document) return@launch
                    val first = hits.isEmpty() && found.matches.isNotEmpty()
                    hits.addAll(found.matches)
                    hasText = hasText || found.hasText
                    state = state.copy(matches = hits.toList(), selected = if (first) 0 else state.selected,
                        searchedPages = page + 1, hasText = hasText, truncated = found.truncated,
                        navigation = state.navigation + if (first) 1 else 0)
                    if (found.truncated) break
                }
                state = state.copy(busy = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (token == generation && session === document) state = state.copy(busy = false, failed = true)
            }
        }
    }
    fun move(direction: Int) {
        if (state.matches.isEmpty()) return
        val next = Math.floorMod(state.selected + direction, state.matches.size)
        state = state.copy(selected = next, navigation = state.navigation + 1)
    }
    companion object { const val MAX_MATCHES = 5000 }
}
