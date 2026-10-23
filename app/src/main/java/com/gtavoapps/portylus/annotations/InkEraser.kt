package com.gtavoapps.portylus.annotations

import kotlin.math.abs
import kotlin.math.sqrt

enum class EraserMode { PARTIAL, WHOLE }
data class EraserOptions(val mode: EraserMode = EraserMode.PARTIAL, val filter: AnnotationKind = AnnotationKind.PEN) {
    init { require(filter == AnnotationKind.PEN || filter == AnnotationKind.HIGHLIGHTER) }
}

/** Clip vector centerlines against the swept circular eraser, including ink's round caps. */
object InkEraser {
    fun erase(strokes: List<Annotation>, start: PagePoint, end: PagePoint, radius: Float,
        options: EraserOptions): List<Annotation> {
        require(radius.isFinite() && radius > 0)
        return strokes.flatMap { stroke ->
            if (stroke.kind != options.filter) listOf(stroke)
            else cut(stroke, start, end, radius + stroke.width / 2, options.mode)
        }
    }

    private fun cut(stroke: Annotation, a: PagePoint, b: PagePoint, radius: Float,
        mode: EraserMode): List<Annotation> {
        var changed = false
        val fragments = mutableListOf<List<PagePoint>>()
        var run = mutableListOf<PagePoint>()
        fun flush() {
            if (run.isNotEmpty()) fragments.add(if (run.size == 1) listOf(run[0], run[0]) else run.toList())
            run = mutableListOf()
        }
        for ((p, q) in stroke.points.zipWithNext()) {
            val cuts = intersections(p, q, a, b, radius)
            if (cuts.isEmpty()) {
                if (run.isEmpty()) run.add(p)
                run.add(q)
                continue
            }
            changed = true
            if (mode == EraserMode.WHOLE) return emptyList()
            var cursor = 0.0
            fun point(t: Double) = PagePoint((p.x + (q.x - p.x) * t).toFloat(), (p.y + (q.y - p.y) * t).toFloat())
            for ((lo, hi) in cuts) {
                if (lo > cursor + 1e-7) {
                    if (run.isEmpty()) run.add(point(cursor))
                    run.add(point(lo))
                }
                flush()
                cursor = hi
            }
            if (cursor < 1.0 - 1e-7) { run.add(point(cursor)); run.add(q) }
        }
        flush()
        if (!changed) return listOf(stroke)
        return fragments.map { stroke.copy(id = java.util.UUID.randomUUID().toString(), points = it) }
    }

    /** Parameter intervals of p..q inside the capsule a..b. No sampling gaps on fast gestures. */
    private fun intersections(p: PagePoint, q: PagePoint, a: PagePoint, b: PagePoint,
        radius: Float): List<Pair<Double, Double>> {
        val dx = (q.x - p.x).toDouble(); val dy = (q.y - p.y).toDouble()
        val ex = (b.x - a.x).toDouble(); val ey = (b.y - a.y).toDouble()
        val length2 = ex * ex + ey * ey
        val r = radius.toDouble()
        val intervals = mutableListOf<Pair<Double, Double>>()
        fun add(lo: Double, hi: Double) {
            val l = lo.coerceAtLeast(0.0); val h = hi.coerceAtMost(1.0)
            if (l <= h) intervals.add(l to h)
        }
        fun circle(c: PagePoint) {
            val x = (p.x - c.x).toDouble(); val y = (p.y - c.y).toDouble()
            val aa = dx * dx + dy * dy
            val cc = x * x + y * y - r * r
            if (aa < 1e-16) { if (cc <= 0) add(0.0, 1.0); return }
            val bb = 2 * (x * dx + y * dy)
            val disc = bb * bb - 4 * aa * cc
            if (disc >= 0) {
                val root = sqrt(disc)
                add((-bb - root) / (2 * aa), (-bb + root) / (2 * aa))
            }
        }
        circle(a)
        if (length2 > 1e-16) {
            circle(b)
            var lo = 0.0; var hi = 1.0
            fun slab(origin: Double, delta: Double, min: Double, max: Double) {
                if (abs(delta) < 1e-16) { if (origin < min || origin > max) { lo = 1.0; hi = 0.0 }; return }
                val t1 = (min - origin) / delta; val t2 = (max - origin) / delta
                lo = maxOf(lo, minOf(t1, t2)); hi = minOf(hi, maxOf(t1, t2))
            }
            val x = (p.x - a.x).toDouble(); val y = (p.y - a.y).toDouble()
            slab(x * ex + y * ey, dx * ex + dy * ey, 0.0, length2)
            val bound = r * sqrt(length2)
            slab(x * ey - y * ex, dx * ey - dy * ex, -bound, bound)
            if (lo <= hi) add(lo, hi)
        }
        val merged = mutableListOf<Pair<Double, Double>>()
        for (interval in intervals.sortedBy { it.first }) {
            val last = merged.lastOrNull()
            if (last != null && interval.first <= last.second + 1e-7)
                merged[merged.lastIndex] = last.first to maxOf(last.second, interval.second)
            else merged.add(interval)
        }
        return merged
    }
}
