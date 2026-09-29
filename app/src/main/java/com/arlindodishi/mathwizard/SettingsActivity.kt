package com.arlindodishi.mathwizard

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchTts = findViewById<SwitchMaterial>(R.id.switchTts)
        val switchHaptics = findViewById<SwitchMaterial>(R.id.switchHaptics)
        val switchContrast = findViewById<SwitchMaterial>(R.id.switchContrast)
        val btnResetScore = findViewById<Button>(R.id.btnResetScore)

        val settingsPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val scoresPrefs = getSharedPreferences("Scores", Context.MODE_PRIVATE)

        // Read saved configs
        switchTts.isChecked = settingsPrefs.getBoolean("tts_enabled", true)
        switchHaptics.isChecked = settingsPrefs.getBoolean("haptics_enabled", true)
        switchContrast.isChecked = settingsPrefs.getBoolean("high_contrast", false)

        applyTheme(switchContrast.isChecked)

        switchTts.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("tts_enabled", isChecked).apply()
        }
        
        switchHaptics.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("haptics_enabled", isChecked).apply()
        }
        
        switchContrast.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("high_contrast", isChecked).apply()
            applyTheme(isChecked)
        }

        btnResetScore.setOnClickListener {
            scoresPrefs.edit()
                .putInt("best_score", 0)
                .putInt("total_played", 0)
                .putInt("total_correct", 0)
                .apply()
            Toast.makeText(this, "Statistics Reset", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun applyTheme(highContrast: Boolean) {
        if (highContrast) {
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.hc_background))
        } else {
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background))
        }
    }
}
