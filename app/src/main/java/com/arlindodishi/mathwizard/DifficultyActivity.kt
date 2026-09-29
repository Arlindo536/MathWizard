package com.arlindodishi.mathwizard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DifficultyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_difficulty)

        // Adjust colors for High Contrast
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("high_contrast", false)) {
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.hc_background))
        }

        findViewById<Button>(R.id.btnEasy).setOnClickListener {
            launchQuiz("EASY")
        }

        findViewById<Button>(R.id.btnMedium).setOnClickListener {
            launchQuiz("MEDIUM")
        }

        findViewById<Button>(R.id.btnHard).setOnClickListener {
            launchQuiz("HARD")
        }
    }

    private fun launchQuiz(difficulty: String) {
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra("DIFFICULTY", difficulty)
        startActivity(intent)
        finish()
    }
}
