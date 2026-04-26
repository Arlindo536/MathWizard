package com.example.mathwizard

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchTts = findViewById<SwitchMaterial>(R.id.switchTts)
        val btnResetScore = findViewById<Button>(R.id.btnResetScore)

        val settingsPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val scoresPrefs = getSharedPreferences("Scores", Context.MODE_PRIVATE)

        switchTts.isChecked = settingsPrefs.getBoolean("tts_enabled", true)

        switchTts.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("tts_enabled", isChecked).apply()
        }

        btnResetScore.setOnClickListener {
            scoresPrefs.edit().putInt("best_score", 0).apply()
            Toast.makeText(this, "High Score Reset", Toast.LENGTH_SHORT).show()
        }
    }
}
