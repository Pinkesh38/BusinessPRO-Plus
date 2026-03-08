package com.example.businessproplus

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFontSettings()
    }

    private fun applyFontSettings() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val fontStyle = prefs.getString("FONT_STYLE", "Default")
        val fontSize = prefs.getString("FONT_SIZE", "Normal")

        // Apply theme-level font size scaling
        val scale = when (fontSize) {
            "Small" -> 0.85f
            "Large" -> 1.15f
            "Extra Large" -> 1.3f
            else -> 1.0f
        }

        val configuration = Configuration(resources.configuration)
        configuration.fontScale = scale
        applyOverrideConfiguration(configuration)
    }

    // Recursively apply font style to all text views
    protected fun applyFontStyle(view: View) {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val fontStyle = prefs.getString("FONT_STYLE", "Default")
        
        val typeface = when (fontStyle) {
            "Monospace" -> Typeface.MONOSPACE
            "Serif" -> Typeface.SERIF
            "Sans-Serif" -> Typeface.SANS_SERIF
            else -> Typeface.DEFAULT
        }

        if (view is TextView) {
            view.typeface = typeface
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontStyle(view.getChildAt(i))
            }
        }
    }
}