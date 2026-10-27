package com.gtavoapps.portylus.pdf

import com.gtavoapps.portylus.R
import android.content.Context
import android.annotation.SuppressLint
import androidx.core.graphics.withClip
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.BlendMode
import android.graphics.RectF
import android.util.LruCache
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import com.gtavoapps.portylus.annotations.Annotation
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.PagePoint
import com.gtavoapps.portylus.annotations.PenSession
import com.gtavoapps.portylus.annotations.InkStyle
import com.gtavoapps.portylus.annotations.EraserOptions
import com.gtavoapps.portylus.annotations.InkEraser

/** Explicit tools: stylus pen, finger straight highlighter, and PDF text selection. */
// Created exclusively by AndroidView with an injected session; never inflated from XML.
@SuppressLint("ViewConstructor")
class PdfReaderView(context: Context, private val session: PdfSession, private val pen: PenSession,
    initialPage: Int = 1) : View(context) {
    private val strings = context
    var fingerWriting = false
        set(value) { if (field != value) cancelStroke(); field = value }
    private var explicitPage: Int? = null
    private var pendingPage: Int? = initialPage.coerceIn(1, session.pageCount) - 1
    var editingLocked = false
        set(value) { if (value && !field) { cancelStroke(); scroller.forceFinished(true) }; field = value }
    var annotationRevision = -1
        set(value) {
            if (field == value) return
            field = value
            cancelStroke()
            paths.clear()
            invalidate()
        }
    var eraserOptions = EraserOptions()
        set(value) { if (field != value) cancelStroke(); field = value }
    var eraserEnabled = false
        set(value) { if (field != value) cancelStroke(); field = value }
    var onPageChanged: (Int) -> Unit = {}
    var onError: (String) -> Unit = {}
    var onTextSelectionChanged: (String) -> Unit = {}
    var penStyle = InkStyle(0xff2255cc.toInt(), 2f)
    var highlighterStyle = InkStyle(0xffffdf00.toInt(), 12f)
    var penEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            cancelStroke()
            invalidate()
        }
    var highlighterEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            cancelStroke()
        }
    var textSelectionEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            cancelStroke()
            clearTextSelection()
        }
    internal var textSelection: TextSelection? = null
        private set
    private data class SelectionRequest(val revision: Long, val page: Int, val start: PagePoint,
        val end: PagePoint, val tolerance: Float, val complete: Boolean)
    private var selectionRequests: Channel<SelectionRequest>? = null
    private var selectionRevision = 0L
    internal val isDocumentReady: Boolean get() = sizes.isNotEmpty()
    private var searchMatches = emptyList<SearchMatch>()
    private var searchByPage = emptyMap<Int, List<SearchMatch>>()
    internal var activeSearchMatch: SearchMatch? = null; private set
    internal val searchHighlightCount: Int get() = searchMatches.size
    private var searchNavigation = Long.MIN_VALUE
    private var pendingSearch: SearchMatch? = null
    private val searchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scope: CoroutineScope? = null
    private var sizes: List<PageSize> = emptyList()
    private var tops = FloatArray(0)
    private var contentHeight = 0f
    private var zoom = 1f
    private var panX = 0f
    private var offsetY = 0f
    private var currentPage = -1
    private val gap = 12f * resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val jobs = mutableMapOf<Int, Job>()
    private val drawRect = RectF()
    private val visiblePages = mutableSetOf<Int>()
    private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val paths = mutableMapOf<String, Path>()
    private var activePointer = MotionEvent.INVALID_POINTER_ID
    private var activePage: PagePlacement? = null
    private var activeKind: AnnotationKind? = null
    private var activeStyle: InkStyle? = null
    private var activeSelecting = false
    private var activeErasing = false
    private var erasedPreview: List<Annotation>? = null
    private var previousSelection: TextSelection? = null
    private var activeTool = MotionEvent.TOOL_TYPE_UNKNOWN
    private val activePoints = mutableListOf<PagePoint>()
    private val activePath = Path()
    private var strokeStoppedAtEdge = false
    private var navigationBlocked = false
    private var navigationStarted = false
    private var fingerNavigation = false
    private var scaleFocusX = 0f
    private var scaleFocusY = 0f
    private val scroller = OverScroller(context)
    private data class RenderedPage(val requestedWidth: Int, val bitmap: Bitmap)
    // Keep the last completed image of each page until its replacement is actually ready.
    private val cache = object : LruCache<Int, RenderedPage>(32 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: RenderedPage) = value.bitmap.allocationByteCount
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            explicitPage = null
            scroller.forceFinished(true)
            scaleFocusX = detector.focusX; scaleFocusY = detector.focusY
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldZoom = zoom
            zoom = (zoom * detector.scaleFactor).coerceIn(1f, 4f)
            val ratio = zoom / oldZoom
            panX = detector.focusX - (scaleFocusX - panX) * ratio
            offsetY = (offsetY + scaleFocusY) * ratio - detector.focusY
            scaleFocusX = detector.focusX; scaleFocusY = detector.focusY
            clamp()
            invalidate()
            return true
        }
    })
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean { scroller.forceFinished(true); return true }
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scaleDetector.isInProgress) return false
            explicitPage = null
            panX -= distanceX
            offsetY += distanceY
            clamp()
            invalidate()
            return true
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            explicitPage = null
            scroller.fling((-panX).toInt(), offsetY.toInt(), -velocityX.toInt(), -velocityY.toInt(),
                0, (width * (zoom - 1)).toInt(), 0, maxOffset().toInt())
            postInvalidateOnAnimation()
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            explicitPage = null
            val old = zoom
            zoom = if (zoom > 1.1f) 1f else 2f
            panX = e.x - (e.x - panX) * (zoom / old)
            offsetY = (offsetY + e.y) * (zoom / old) - e.y
            clamp(); invalidate()
            return true
        }
        override fun onSingleTapUp(e: MotionEvent): Boolean { performClick(); return true }
    })

    init { contentDescription = strings.getString(R.string.reader_description) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val attachedScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = attachedScope
        val requests = Channel<SelectionRequest>(Channel.CONFLATED)
        selectionRequests = requests
        attachedScope.launch {
            for (request in requests) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        session.selectText(request.page, request.start, request.end, request.tolerance)
                    }
                    if (request.revision != selectionRevision || !textSelectionEnabled) continue
                    textSelection = result.takeIf { it.text.isNotEmpty() }
                    if (request.complete) {
                        onTextSelectionChanged(result.text)
                        if (result.text.isEmpty()) onError(strings.getString(R.string.no_selectable_text))
                    }
                    invalidate()
                } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (_: Exception) {
                    if (request.revision == selectionRevision) {
                        clearTextSelection()
                        onError(strings.getString(R.string.selection_error))
                    }
                }
            }
        }
        attachedScope.launch {
            try {
                sizes = withContext(Dispatchers.IO) { (0 until session.pageCount).map(session::pageSize) }
                layoutPages(); invalidate()
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) { onError(strings.getString(R.string.render_error)) }
        }
    }
    override fun onDetachedFromWindow() {
        cancelStroke()
        clearTextSelection()
        selectionRequests?.close(); selectionRequests = null
        navigationStarted = false
        navigationBlocked = false
        scope?.cancel(); scope = null
        jobs.clear(); cache.evictAll(); paths.clear()
        super.onDetachedFromWindow()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cancelStroke()
        val ratio = if (oldw > 0) w.toFloat() / oldw else 1f
        offsetY *= ratio
        layoutPages(); clamp()
    }
    private fun layoutPages() {
        var y = gap
        tops = FloatArray(sizes.size) { index ->
            val top = y
            val size = sizes[index]
            y += width * size.height / size.width + gap
            top
        }
        contentHeight = y
        if (width > 0 && height > 0 && sizes.isNotEmpty()) pendingPage?.let {
            offsetY = (tops[it] * zoom - gap).coerceIn(0f, maxOffset())
            pendingPage = null
            currentPage = it
            onPageChanged(it + 1)
        }
        focusSearch()
    }
    /** A direct jump preserves zoom, cancels provisional input and never writes annotations. */
    fun goToPage(page: Int) {
        require(page in 1..session.pageCount)
        cancelInteraction()
        clearTextSelection()
        pendingSearch = null
        explicitPage = page - 1
        pendingPage = page - 1
        layoutPages()
        clamp()
        invalidate()
    }

    /** Updates an overlay only; search can never become a saved annotation. */
    fun showSearch(matches: List<SearchMatch>, selected: Int, navigation: Long) {
        if (searchMatches !== matches) {
            searchMatches = matches
            searchByPage = matches.groupBy { it.page }
        }
        activeSearchMatch = matches.getOrNull(selected)
        if (navigation != searchNavigation) {
            searchNavigation = navigation
            pendingSearch = activeSearchMatch
            focusSearch()
        }
        if (matches.isEmpty()) pendingSearch = null
        invalidate()
    }
    private fun focusSearch() {
        val match = pendingSearch ?: return
        if (width <= 0 || height <= 0 || match.page !in sizes.indices) return
        val points = match.quads.flatMap { listOf(it.upperLeft, it.upperRight, it.lowerLeft, it.lowerRight) }
        if (points.isEmpty()) return
        explicitPage = null
        val size = sizes[match.page]
        val scale = width * zoom / size.width
        val x = (points.minOf { it.x } + points.maxOf { it.x }) / 2
        val y = (points.minOf { it.y } + points.maxOf { it.y }) / 2
        scroller.forceFinished(true)
        offsetY = tops[match.page] * zoom + (y - size.originY) * scale - height * 0.35f
        panX = width / 2f - (x - size.originX) * scale
        clamp()
        pendingSearch = null
        currentPage = match.page
        onPageChanged(match.page + 1)
        invalidate()
    }
    private fun drawSearch(canvas: Canvas, page: PagePlacement) {
        val hits = searchByPage[page.page] ?: return
        canvas.withClip(page.left, page.top, page.right, page.bottom) {
        val path = Path()
        for (hit in hits) {
            searchPaint.color = if (hit === activeSearchMatch) 0x88ff8a24.toInt() else 0x55ffd54f
            for (quad in hit.quads) {
                val points = listOf(quad.upperLeft, quad.upperRight, quad.lowerRight, quad.lowerLeft).map(page::toScreen)
                path.reset(); path.moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                path.close(); canvas.drawPath(path, searchPaint)
            }
        }
        }
    }

    private fun maxOffset() = (contentHeight * zoom - height).coerceAtLeast(0f)
    private fun clamp() {
        offsetY = offsetY.coerceIn(0f, maxOffset())
        panX = panX.coerceIn(-width * (zoom - 1f), 0f)
    }
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            panX = -scroller.currX.toFloat(); offsetY = scroller.currY.toFloat()
            postInvalidateOnAnimation()
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(6, 20, 38))
        if (sizes.isEmpty()) {
            paint.color = Color.LTGRAY; paint.textSize = 16 * resources.displayMetrics.density
            canvas.drawText(strings.getString(R.string.opening), gap * 2, gap * 4, paint)
            return
        }
        var bestPage = 0
        var bestArea = 0f
        visiblePages.clear()
        for (index in sizes.indices) {
            val top = tops[index] * zoom - offsetY
            val pageHeight = width * sizes[index].height / sizes[index].width * zoom
            if (top > height || top + pageHeight < 0) continue
            val rect = drawRect.apply { set(panX, top, panX + width * zoom, top + pageHeight) }
            paint.color = Color.WHITE
            canvas.drawRect(rect, paint)
            // Small span fluctuations should not request a different bitmap for every screen pixel.
            val requestedWidth = (((width * zoom).toInt().coerceIn(1, 2400) + 127) / 128 * 128).coerceAtMost(2400)
            visiblePages.add(index)
            val rendered = cache.get(index)
            if (rendered != null) canvas.drawBitmap(rendered.bitmap, null, rect, paint)
            if (rendered?.requestedWidth != requestedWidth) requestPage(index, requestedWidth)
            drawInk(canvas, placement(index))
            drawTextSelection(canvas, placement(index))
            drawSearch(canvas, placement(index))
            val area = minOf(top + pageHeight, height.toFloat()) - maxOf(top, 0f)
            if (area > bestArea) { bestArea = area; bestPage = index }
        }
        // Never let a long fling leave an unbounded queue of obsolete page renders.
        jobs.keys.filter { it !in visiblePages }.forEach { jobs.remove(it)?.cancel() }
        // A short final page may occupy less screen area than its neighbour after a jump.
        // Keep the requested page until the reader actually scrolls or zooms again.
        val displayedPage = explicitPage?.takeIf { it in visiblePages } ?: bestPage
        if (displayedPage != currentPage) { currentPage = displayedPage; onPageChanged(displayedPage + 1) }
    }

    private fun placement(index: Int) = PagePlacement(index, panX, tops[index] * zoom - offsetY,
        width * zoom / sizes[index].width, sizes[index])

    internal fun pageAt(x: Float, y: Float): Pair<PagePlacement, PagePoint>? {
        if (width <= 0) return null
        for (index in sizes.indices) {
            val page = placement(index)
            val point = page.toPage(x, y) ?: continue
            return page to point
        }
        return null
    }

    private fun drawInk(canvas: Canvas, page: PagePlacement) {
        val checkpoint = canvas.save()
        canvas.clipRect(page.left, page.top, page.right, page.bottom)
        canvas.translate(page.left, page.top)
        canvas.scale(page.scale, page.scale)
        canvas.translate(-page.size.originX, -page.size.originY)
        for (stroke in (erasedPreview?.takeIf { activePage?.page == page.page } ?: pen.strokes(page.page))) {
            setInkStyle(stroke.kind, stroke.color, stroke.width)
            val path = paths.getOrPut(stroke.id) {
                Path().apply {
                    moveTo(stroke.points.first().x, stroke.points.first().y)
                    stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                }
            }
            canvas.drawPath(path, inkPaint)
            // A tap is also a legitimate pen dot (a zero-length Path is not consistently painted).
            if (stroke.points.all { it == stroke.points.first() })
                canvas.drawPoint(stroke.points.first().x, stroke.points.first().y, inkPaint)
        }
        if (!activeSelecting && !activeErasing && activePage?.page == page.page && activePoints.isNotEmpty()) {
            val style = requireNotNull(activeStyle)
            setInkStyle(requireNotNull(activeKind), style.color, style.width)
            canvas.drawPath(activePath, inkPaint)
            if (activePoints.size == 1) canvas.drawPoint(activePoints[0].x, activePoints[0].y, inkPaint)
        }
        if (activeErasing && activePage?.page == page.page && activePoints.isNotEmpty()) {
            val point = activePoints.last()
            inkPaint.blendMode = BlendMode.SRC_OVER; inkPaint.color = Color.rgb(255, 168, 64)
            inkPaint.alpha = 255; inkPaint.strokeWidth = 1.5f * resources.displayMetrics.density / page.scale
            canvas.drawCircle(point.x, point.y, eraserRadius(page), inkPaint)
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun setInkStyle(kind: AnnotationKind, color: Int, width: Float) {
        inkPaint.color = color
        inkPaint.alpha = if (kind == AnnotationKind.HIGHLIGHTER) 72 else 255
        inkPaint.blendMode = if (kind == AnnotationKind.HIGHLIGHTER) BlendMode.MULTIPLY else BlendMode.SRC_OVER
        inkPaint.strokeWidth = width
    }

    private fun drawTextSelection(canvas: Canvas, page: PagePlacement) {
        val selection = textSelection?.takeIf { it.page == page.page } ?: return
        val checkpoint = canvas.save()
        canvas.clipRect(page.left, page.top, page.right, page.bottom)
        paint.color = 0x66338fff
        paint.style = Paint.Style.FILL
        val path = Path()
        for (quad in selection.quads) {
            val points = listOf(quad.upperLeft, quad.upperRight, quad.lowerRight, quad.lowerLeft).map(page::toScreen)
            path.reset(); path.moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }
            path.close(); canvas.drawPath(path, paint)
        }
        // Bitmap drawing uses this Paint too; do not leak the selection alpha into page rendering.
        paint.alpha = 255
        canvas.restoreToCount(checkpoint)
    }

    fun cancelInteraction() { cancelStroke(); scroller.forceFinished(true) }

    fun clearTextSelection() {
        selectionRevision++
        textSelection = null
        onTextSelectionChanged("")
        invalidate()
    }

    private fun requestTextSelection(complete: Boolean) {
        val page = activePage ?: return
        if (activePoints.isEmpty()) return
        selectionRevision++
        selectionRequests?.trySend(SelectionRequest(selectionRevision, page.page, activePoints.first(),
            activePoints.last(), 12f * resources.displayMetrics.density / page.scale, complete))
    }

    private fun eraserRadius(page: PagePlacement) = 12f * resources.displayMetrics.density / page.scale

    private fun eraseTo(point: PagePoint) {
        val page = activePage ?: return
        erasedPreview = InkEraser.erase(erasedPreview ?: pen.strokes(page.page),
            activePoints.lastOrNull() ?: point, point, eraserRadius(page), eraserOptions)
        paths.clear()
    }

    private fun beginStroke(event: MotionEvent, index: Int, kind: AnnotationKind? = null,
        selecting: Boolean = false, erasing: Boolean = false) {
        if (activePointer != MotionEvent.INVALID_POINTER_ID) return
        val hit = pageAt(event.getX(index), event.getY(index)) ?: return
        activePointer = event.getPointerId(index)
        activePage = hit.first
        activeKind = kind
        activeStyle = when (kind) {
            AnnotationKind.PEN -> penStyle
            AnnotationKind.HIGHLIGHTER -> highlighterStyle
            else -> null
        }
        activeSelecting = selecting
        activeErasing = erasing
        activeTool = event.getToolType(index)
        if (selecting) {
            previousSelection = textSelection
            clearTextSelection()
        }
        activePoints.clear()
        activePoints.add(hit.second)
        if (erasing) eraseTo(hit.second)
        activePath.reset()
        activePath.moveTo(hit.second.x, hit.second.y)
        strokeStoppedAtEdge = false
        parent?.requestDisallowInterceptTouchEvent(true)
        invalidate()
    }

    private fun appendSamples(event: MotionEvent, index: Int) {
        val page = activePage ?: return
        fun append(x: Float, y: Float) {
            if (strokeStoppedAtEdge) return
            val point = page.toPage(x, y)
            if (point == null) { strokeStoppedAtEdge = true; return }
            if (activePoints.lastOrNull() == point) return
            if (activeErasing) {
                eraseTo(point)
                activePoints.clear(); activePoints.add(point)
                return
            }
            if (activeKind == AnnotationKind.HIGHLIGHTER || activeSelecting) {
                // The preview and stored stroke contain only the anchor and current endpoint.
                if (activePoints.size == 1) activePoints.add(point) else activePoints[1] = point
                activePath.reset()
                activePath.moveTo(activePoints.first().x, activePoints.first().y)
            } else activePoints.add(point)
            activePath.lineTo(point.x, point.y)
        }
        for (history in 0 until event.historySize)
            append(event.getHistoricalX(index, history), event.getHistoricalY(index, history))
        append(event.getX(index), event.getY(index))
        if (activeSelecting) requestTextSelection(complete = false)
        invalidate()
    }

    private fun finishStroke() {
        val page = activePage
        if (page != null && activePoints.isNotEmpty()) {
            if (activeErasing) {
                erasedPreview?.let { pen.replace(page.page, it) }
                // Acknowledge our own edit now: a delayed Compose update must not cancel the next gesture.
                annotationRevision = pen.revision
                cancelStroke()
                return
            }
            if (activeSelecting) {
                requestTextSelection(complete = true)
                // Preserve the final asynchronous request while releasing the pointer.
                activeSelecting = false
                cancelStroke()
                return
            }
            val points = if (activePoints.size == 1) listOf(activePoints[0], activePoints[0]) else activePoints.toList()
            val kind = requireNotNull(activeKind)
            val style = requireNotNull(activeStyle)
            pen.add(Annotation(page = page.page, kind = kind, points = points,
                color = style.color, width = style.width))
            annotationRevision = pen.revision
        }
        cancelStroke()
    }

    private fun cancelStroke(restoreSelection: Boolean = false) {
        if (activeSelecting) {
            val previous = previousSelection
            clearTextSelection()
            if (restoreSelection) {
                textSelection = previous
                onTextSelectionChanged(previous?.text.orEmpty())
            }
        }
        previousSelection = null
        activePointer = MotionEvent.INVALID_POINTER_ID
        activePage = null
        activeKind = null
        activeStyle = null
        activeSelecting = false
        activeErasing = false
        erasedPreview = null
        activeTool = MotionEvent.TOOL_TYPE_UNKNOWN
        activePoints.clear()
        activePath.reset()
        strokeStoppedAtEdge = false
        parent?.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    private fun cancelNavigation(event: MotionEvent) {
        scroller.forceFinished(true)
        if (!navigationStarted) return
        val cancel = MotionEvent.obtain(event)
        try {
            cancel.action = MotionEvent.ACTION_CANCEL
            scaleDetector.onTouchEvent(cancel)
            gestures.onTouchEvent(cancel)
        } finally { cancel.recycle() }
        navigationStarted = false
    }
    private fun requestPage(index: Int, renderWidth: Int) {
        // Finish in-flight work for a visible page even if the requested zoom has changed.
        // The next frame requests the latest resolution, with this result available as a fallback.
        if (jobs.containsKey(index)) return
        val attachedScope = scope ?: return
        val renderJob = attachedScope.launch(start = CoroutineStart.LAZY) {
            try {
                val bitmap = withContext(Dispatchers.IO) { session.render(index, renderWidth) }
                cache.put(index, RenderedPage(renderWidth, bitmap)); invalidate()
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) { onError(strings.getString(R.string.page_error)) }
        }
        jobs[index] = renderJob
        renderJob.invokeOnCompletion { if (jobs[index] === renderJob) jobs.remove(index) }
        renderJob.start()
    }
    // GestureDetector dispatches actual taps to onSingleTapUp -> performClick above.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editingLocked) return true
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            cancelStroke()
            navigationBlocked = false
            navigationStarted = false
            fingerNavigation = false
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelStroke()
            cancelNavigation(event)
            navigationBlocked = false
            return true
        }
        val hasNonFinger = (0 until event.pointerCount).any {
            event.getToolType(it) != MotionEvent.TOOL_TYPE_FINGER
        }
        if (hasNonFinger) {
            // Keep the finger/palm tail quarantined, even after the stylus pointer goes up.
            navigationBlocked = true
            cancelNavigation(event)
            // A finger mark may have been a palm arriving before the stylus.
            if (activeTool == MotionEvent.TOOL_TYPE_FINGER) cancelStroke()
        }
        val changed = event.actionIndex
        val fingers = (0 until event.pointerCount).count { event.getToolType(it) == MotionEvent.TOOL_TYPE_FINGER }
        if (!navigationBlocked && fingers >= 2 && !fingerNavigation) {
            cancelStroke(restoreSelection = true)
            fingerNavigation = true
            // A tool consumed the first DOWN. Give both gesture detectors a fresh navigation start.
            if (!navigationStarted) {
                val down = MotionEvent.obtain(event.downTime, event.eventTime, MotionEvent.ACTION_DOWN,
                    event.getX(0), event.getY(0), 0)
                try { scaleDetector.onTouchEvent(down); gestures.onTouchEvent(down) }
                finally { down.recycle() }
                navigationStarted = true
            }
        }
        if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) &&
            (event.getToolType(changed) == MotionEvent.TOOL_TYPE_STYLUS ||
                event.getToolType(changed) == MotionEvent.TOOL_TYPE_ERASER)) {
            if (eraserEnabled) beginStroke(event, changed, erasing = true)
            else if (event.getToolType(changed) == MotionEvent.TOOL_TYPE_ERASER) Unit
            else if (textSelectionEnabled) beginStroke(event, changed, selecting = true)
            else if (penEnabled) beginStroke(event, changed, AnnotationKind.PEN)
        } else if (action == MotionEvent.ACTION_DOWN && !navigationBlocked && !fingerNavigation &&
            event.getToolType(changed) == MotionEvent.TOOL_TYPE_FINGER) {
            if (eraserEnabled) beginStroke(event, changed, erasing = true)
            else if (textSelectionEnabled) beginStroke(event, changed, selecting = true)
            else if (highlighterEnabled) beginStroke(event, changed, AnnotationKind.HIGHLIGHTER)
            else if (com.gtavoapps.portylus.input.InputPolicy.action(com.gtavoapps.portylus.input.Tool.FINGER,
                com.gtavoapps.portylus.input.ToolState(penEnabled, highlighterEnabled, fingerWriting), fingers, false)
                == com.gtavoapps.portylus.input.InputAction.PEN) beginStroke(event, changed, AnnotationKind.PEN)
        }
        if (activePointer != MotionEvent.INVALID_POINTER_ID) {
            val index = event.findPointerIndex(activePointer)
            if (index < 0 || event.getToolType(index) != activeTool) cancelStroke()
            else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) &&
                event.getPointerId(changed) == activePointer) {
                if (event.flags and MotionEvent.FLAG_CANCELED != 0) cancelStroke()
                else { appendSamples(event, index); finishStroke() }
            } else if (action == MotionEvent.ACTION_MOVE) appendSamples(event, index)
        }
        if (!navigationBlocked && !hasNonFinger && activePointer == MotionEvent.INVALID_POINTER_ID &&
            (fingerNavigation || (!highlighterEnabled && !textSelectionEnabled && !eraserEnabled && !(penEnabled && fingerWriting)))) {
            if (action == MotionEvent.ACTION_DOWN) navigationStarted = true
            if (navigationStarted) {
                scaleDetector.onTouchEvent(event)
                if (!scaleDetector.isInProgress) gestures.onTouchEvent(event)
            }
        }
        if (action == MotionEvent.ACTION_UP) {
            cancelStroke()
            navigationStarted = false
            navigationBlocked = false
        }
        return true
    }
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (!hasWindowFocus) { cancelStroke(); scroller.forceFinished(true) }
    }
    override fun performClick(): Boolean { super.performClick(); return true }

}
