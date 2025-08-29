package com.example.qare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        val title = findViewById<TextView>(R.id.title)
        val version = findViewById<TextView>(R.id.version)
        val footer = findViewById<TextView>(R.id.footer)

        // CRAZY text animation: pop, wobble, color pulse, letter-spacing sweep
        title.alpha = 0f
        title.scaleX = 0.6f
        title.scaleY = 0.6f
        title.letterSpacing = 0.15f
        title.rotation = -8f

        // Pop in
        title.animate()
            .alpha(1f)
            .scaleX(1.08f)
            .scaleY(1.08f)
            .rotation(0f)
            .setDuration(420)
            .setInterpolator(OvershootInterpolator(2.2f))
            .withEndAction {
                // Micro-bounce
                title.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            .start()

        // Letter spacing sweep and color pulse using postDelayed steps
        val handler = Handler(Looper.getMainLooper())
        val colors = intArrayOf(0xFFE11D48.toInt(), 0xFF22C55E.toInt(), 0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt())
        for (i in 0..6) {
            handler.postDelayed({
                val ls = if (i % 2 == 0) 0.25f else 0.05f
                title.letterSpacing = ls
                title.setTextColor(colors[i % colors.size])
                title.rotation = if (i % 2 == 0) 6f else -6f
                title.animate().rotation(0f).setDuration(140).start()
            }, (480 + i * 120).toLong())
        }

        Handler(Looper.getMainLooper()).postDelayed({
            version.animate().alpha(1f).setDuration(450).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }, 900)

        Handler(Looper.getMainLooper()).postDelayed({
            footer.animate().alpha(1f).setDuration(450).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }, 1200)

        // Navigate after animations
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2200)
    }
}


