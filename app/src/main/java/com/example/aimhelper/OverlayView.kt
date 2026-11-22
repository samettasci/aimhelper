package com.example.aimhelper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class OverlayView(context: Context) : View(context) {

    var editMode = true
        set(value) { field = value; invalidate() }

    var maxBounces = 4

    private val density = resources.displayMetrics.density
    private val table = RectF()
    private val cue = PointF()
    private val aim = PointF()
    private var initialized = false
    private var active = -1 // 0 = sol-üst, 1 = sağ-alt, 2 = beyaz top, 3 = nişan

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 255, 255)
        strokeWidth = 2.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val bouncePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 255, 80, 80)
        style = Paint.Style.FILL
    }
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 80, 160, 255)
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (!initialized && w > 0 && h > 0) {
            table.set(w * 0.12f, h * 0.22f, w * 0.88f, h * 0.78f)
            cue.set(w * 0.35f, h * 0.5f)
            aim.set(w * 0.65f, h * 0.4f)
            initialized = true
        }
    }

    /** Başlangıçtan yöne doğru ilerler, masa dikdörtgeninin kenarlarından yansıtır. */
    private fun computePath(): List<PointF> {
        val pts = mutableListOf(PointF(cue.x, cue.y))
        var dx = aim.x - cue.x
        var dy = aim.y - cue.y
        val len = hypot(dx, dy)
        if (len < 1f) return pts
        dx /= len; dy /= len

        var px = cue.x.coerceIn(table.left, table.right)
        var py = cue.y.coerceIn(table.top, table.bottom)

        repeat(maxBounces + 1) {
            val tx = when {
                dx > 1e-6f -> (table.right - px) / dx
                dx < -1e-6f -> (table.left - px) / dx
                else -> Float.MAX_VALUE
            }
            val ty = when {
                dy > 1e-6f -> (table.bottom - py) / dy
                dy < -1e-6f -> (table.top - py) / dy
                else -> Float.MAX_VALUE
            }
            val t = minOf(tx, ty)
            if (t == Float.MAX_VALUE || t < 0f) return pts
            px += dx * t
            py += dy * t
            pts.add(PointF(px, py))
            if (tx < ty) dx = -dx else dy = -dy
        }
        return pts
    }

    override fun onDraw(canvas: Canvas) {
        if (!initialized) return

        val pts = computePath()
        for (i in 0 until pts.size - 1) {
            canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, linePaint)
        }
        for (i in 1 until pts.size) {
            canvas.drawCircle(pts[i].x, pts[i].y, 4f * density, bouncePaint)
        }

        if (editMode) {
            canvas.drawRect(table, tablePaint)
            handle(canvas, table.left, table.top, Color.argb(220, 80, 160, 255))
            handle(canvas, table.right, table.bottom, Color.argb(220, 80, 160, 255))
            handle(canvas, cue.x, cue.y, Color.argb(160, 255, 255, 255))
            handle(canvas, aim.x, aim.y, Color.argb(220, 255, 210, 0))
        }
    }

    private fun handle(c: Canvas, x: Float, y: Float, color: Int) {
        handlePaint.color = color
        c.drawCircle(x, y, 14f * density, handlePaint)
        c.drawCircle(x, y, 14f * density, ringPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!editMode) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val handles = listOf(
                    PointF(table.left, table.top),
                    PointF(table.right, table.bottom),
                    cue, aim
                )
                var best = -1
                var bestD = 56f * density
                handles.forEachIndexed { i, p ->
                    val d = hypot(e.x - p.x, e.y - p.y)
                    if (d < bestD) { bestD = d; best = i }
                }
                // Hiçbir tutamaça yakın değilse: nişan noktasını doğrudan oraya taşı
                if (best == -1) { aim.set(e.x, e.y); best = 3 }
                active = best
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                when (active) {
                    0 -> table.set(minOf(e.x, table.right - 50f), minOf(e.y, table.bottom - 50f), table.right, table.bottom)
                    1 -> table.set(table.left, table.top, maxOf(e.x, table.left + 50f), maxOf(e.y, table.top + 50f))
                    2 -> cue.set(e.x, e.y)
                    3 -> aim.set(e.x, e.y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> active = -1
        }
        return true
    }
}
