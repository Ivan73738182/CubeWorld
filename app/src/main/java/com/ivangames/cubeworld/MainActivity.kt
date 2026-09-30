package com.ivangames.cubeworld

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Полный экран (старый способ — работает везде)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "ОШИБКА:\n\n${e.message}\n\n${e.stackTraceToString().take(1500)}"
            tv.setTextColor(Color.RED)
            tv.setBackgroundColor(Color.BLACK)
            tv.setPadding(40, 40, 40, 40)
            tv.textSize = 14f
            setContentView(tv)
        }
    }
}
