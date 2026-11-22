package com.example.aimhelper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }

        val info = TextView(this).apply {
            text = "Aim Helper (sadece offline antrenman)\n\n" +
                "1) Overlay iznini ver\n" +
                "2) Başlat'a bas, oyunu aç\n" +
                "3) Sağ üstteki panelden AYAR / OYNA arasında geçiş yap\n\n" +
                "AYAR modunda: masa köşelerini (mavi), beyaz topu (beyaz) ve " +
                "nişan noktasını (sarı) sürükle. OYNA modunda dokunuşlar oyuna geçer."
            textSize = 15f
        }

        val perm = Button(this).apply {
            text = "Overlay iznini ver"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        val start = Button(this).apply {
            text = "Başlat"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "Önce overlay izni ver", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                }
                startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        val stop = Button(this).apply {
            text = "Durdur"
            setOnClickListener {
                stopService(Intent(this@MainActivity, OverlayService::class.java))
            }
        }

        root.addView(info)
        root.addView(perm)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }
}
