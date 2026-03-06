package com.example.businessproplus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_splash)

            val ivSplashGif = findViewById<ImageView>(R.id.ivSplashGif)
            
            // 🛡️ SAFE LOADING: Use lower resolution and optimized caching for low-end hardware
            if (ivSplashGif != null) {
                Glide.with(this)
                    .asGif()
                    .load(R.drawable.welcome_anim)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .override(250, 250) 
                    .into(ivSplashGif)
            }

            findViewById<android.view.View>(R.id.main)?.let { v ->
                ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                    insets
                }
            }

            // 🛡️ FAIL-SAFE NAVIGATION: Non-blocking DB init and guaranteed transition
            lifecycleScope.launch {
                // Background DB initialization
                withContext(Dispatchers.IO) {
                    try {
                        AppDatabase.getDatabase(applicationContext).query("SELECT 1", null).close()
                    } catch (e: Exception) {
                        Log.e("SplashActivity", "Database Pre-warm failed", e)
                    }
                }

                // Shorter delay for better UX, then proceed
                delay(2000) 
                proceedToNextScreen()
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Splash Init Error", e)
            proceedToNextScreen()
        }
    }

    private fun proceedToNextScreen() {
        if (isFinishing || isDestroyed) return
        
        try {
            val appPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val isSetupComplete = appPrefs.getBoolean("IS_SETUP_COMPLETE", false)

            val destination = if (!isSetupComplete) {
                SetupActivity::class.java
            } else {
                LoginActivity::class.java
            }

            val intent = Intent(this, destination)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("SplashActivity", "Navigation Failed", e)
            // Last resort fallback
            try {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } catch (ex: Exception) {}
        }
    }
}