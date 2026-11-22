package com.example.aimhelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var overlay: OverlayView? = null
    private var panel: LinearLayout? = null
    private lateinit var overlayParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Tam ekran çizim katmanı
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type, baseFlags, PixelFormat.TRANSLUCENT
        )
        val view = OverlayView(this)
        overlay = view
        wm.addView(view, overlayParams)

        // Küçük kontrol paneli (her zaman dokunulabilir)
        val density = resources.displayMetrics.density
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(170, 0, 0, 0))
        }
        val modeBtn = Button(this).apply {
            text = "OYNA"
            textSize = 12f
            setOnClickListener {
                val edit = !view.editMode
                setEditMode(edit)
                text = if (edit) "OYNA" else "AYAR"
            }
        }
        val bounceBtn = Button(this).apply {
            text = "Yansıma: ${view.maxBounces}"
            textSize = 12f
            setOnClickListener {
                view.maxBounces = if (view.maxBounces >= 6) 1 else view.maxBounces + 1
                text = "Yansıma: ${view.maxBounces}"
                view.invalidate()
            }
        }
        val closeBtn = Button(this).apply {
            text = "X"
            textSize = 12f
            setOnClickListener { stopSelf() }
        }
        p.addView(modeBtn)
        p.addView(bounceBtn)
        p.addView(closeBtn)
        panel = p

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type, baseFlags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (8 * density).toInt()
            y = (8 * density).toInt()
        }
        wm.addView(p, panelParams)

        setEditMode(true)
    }

    /** AYAR modunda katman dokunuşu yakalar, OYNA modunda oyuna geçirir. */
    private fun setEditMode(edit: Boolean) {
        val v = overlay ?: return
        v.editMode = edit
        overlayParams.flags = if (edit) {
            overlayParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            overlayParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        wm.updateViewLayout(v, overlayParams)
    }

    private fun startAsForeground() {
        val channelId = "aimhelper"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Aim Helper", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, channelId)
            .setContentTitle("Aim Helper çalışıyor")
            .setContentText("Overlay aktif (offline antrenman)")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, n)
    }

    override fun onDestroy() {
        overlay?.let { wm.removeView(it) }
        panel?.let { wm.removeView(it) }
        overlay = null
        panel = null
        super.onDestroy()
    }
}
