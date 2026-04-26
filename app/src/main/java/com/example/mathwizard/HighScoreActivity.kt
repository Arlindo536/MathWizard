package com.example.mathwizard

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HighScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        val tvBestScore = findViewById<TextView>(R.id.tvBestScore)

        val scoresPrefs = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        val bestScore = scoresPrefs.getInt("best_score", 0)

        tvBestScore.text = getString(R.string.best_score_label, bestScore)
    }
}
