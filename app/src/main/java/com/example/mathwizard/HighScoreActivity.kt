package com.example.mathwizard

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class HighScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        // Theme Support
        val settingsPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val highContrast = settingsPrefs.getBoolean("high_contrast", false)
        if (highContrast) {
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.hc_background))
        }

        val tvBestScore = findViewById<TextView>(R.id.tvBestScore)
        val tvTotalPlayed = findViewById<TextView>(R.id.tvTotalPlayed)
        val tvAccuracy = findViewById<TextView>(R.id.tvAccuracy)
        val tvHighScoreTitle = findViewById<TextView>(R.id.tvHighScoreTitle)

        if (highContrast) {
            tvHighScoreTitle.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
            tvBestScore.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
            tvTotalPlayed.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
            tvAccuracy.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
        }

        val scoresPrefs = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        val bestScore = scoresPrefs.getInt("best_score", 0)
        val totalPlayed = scoresPrefs.getInt("total_played", 0)
        val totalCorrect = scoresPrefs.getInt("total_correct", 0)

        // Calculate accuracy safely
        val accuracy = if (totalPlayed > 0) {
            ((totalCorrect.toFloat() / totalPlayed.toFloat()) * 100).toInt()
        } else {
            0
        }

        tvBestScore.text = getString(R.string.best_score_label, bestScore)
        tvTotalPlayed.text = getString(R.string.stats_played, totalPlayed)
        tvAccuracy.text = getString(R.string.stats_accuracy, accuracy)
    }
}
