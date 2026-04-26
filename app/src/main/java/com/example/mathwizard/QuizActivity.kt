package com.example.mathwizard

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class QuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val viewModel: QuizViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var hapticsEnabled = false
    private var highContrast = false
    
    // UI Elements
    private lateinit var tvScore: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    
    private var allowClick = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        tvScore = findViewById(R.id.tvScore)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvFeedback = findViewById(R.id.tvFeedback)
        btnAnswer1 = findViewById(R.id.btnAnswer1)
        btnAnswer2 = findViewById(R.id.btnAnswer2)
        btnAnswer3 = findViewById(R.id.btnAnswer3)
        btnAnswer4 = findViewById(R.id.btnAnswer4)

        // Read Settings
        val sharedPrefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val ttsEnabled = sharedPrefs.getBoolean("tts_enabled", true)
        hapticsEnabled = sharedPrefs.getBoolean("haptics_enabled", true)
        highContrast = sharedPrefs.getBoolean("high_contrast", false)

        if (ttsEnabled) {
            tts = TextToSpeech(this, this)
        }
        
        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Apply High Contrast UI
        if (highContrast) {
            window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.hc_background))
            tvQuestion.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
            tvScore.setTextColor(ContextCompat.getColor(this, R.color.hc_text))
        }

        // Retrieve Difficulty
        val intentDiff = intent.getStringExtra("DIFFICULTY")
        if (intentDiff != null) {
            viewModel.difficulty = intentDiff
            viewModel.generateNextQuestion()
        }

        viewModel.score.observe(this) { score ->
            tvScore.text = getString(R.string.score_label, score)
        }

        viewModel.currentQuestion.observe(this) { question ->
            tvQuestion.text = question.text
            btnAnswer1.text = question.options[0].toString()
            btnAnswer2.text = question.options[1].toString()
            btnAnswer3.text = question.options[2].toString()
            btnAnswer4.text = question.options[3].toString()
            
            allowClick = true
            tvFeedback.visibility = View.INVISIBLE
            resetButtonColors()

            if (ttsEnabled && tts != null) {
                var textToRead = question.text.replace("+", "plus").replace("-", "minus").replace("*", "times")
                tts?.speak("What is $textToRead", TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }

        val buttons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)
        for (btn in buttons) {
            btn.setOnClickListener { view ->
                if (!allowClick) return@setOnClickListener
                allowClick = false
                
                val ans = (view as Button).text.toString().toInt()
                val isCorrect = viewModel.checkAnswer(ans)
                
                if (isCorrect) {
                    view.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_color))
                    tvFeedback.text = getString(R.string.correct_answer)
                    tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.correct_color))
                    triggerVibration(true)
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong_color))
                    tvFeedback.text = getString(R.string.wrong_answer)
                    tvFeedback.setTextColor(ContextCompat.getColor(this, R.color.wrong_color))
                    triggerVibration(false)
                }
                tvFeedback.visibility = View.VISIBLE
                
                Handler(Looper.getMainLooper()).postDelayed({
                    viewModel.generateNextQuestion()
                }, 1500)
            }
        }
    }
    
    private fun triggerVibration(isCorrect: Boolean) {
        if (!hapticsEnabled || vibrator == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isCorrect) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
            }
        } else {
            @Suppress("DEPRECATION")
            if (isCorrect) vibrator?.vibrate(100) else vibrator?.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }
    
    private fun resetButtonColors() {
        val defaultColor = if (highContrast) ContextCompat.getColor(this, R.color.hc_button) 
                           else ContextCompat.getColor(this, R.color.primary)
        btnAnswer1.setBackgroundColor(defaultColor)
        btnAnswer2.setBackgroundColor(defaultColor)
        btnAnswer3.setBackgroundColor(defaultColor)
        btnAnswer4.setBackgroundColor(defaultColor)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    override fun onDestroy() {
        val currentScore = viewModel.score.value ?: 0
        val sharedPrefs = getSharedPreferences("Scores", Context.MODE_PRIVATE)
        val bestScore = sharedPrefs.getInt("best_score", 0)
        
        // Save Best Score
        if (currentScore > bestScore) {
            sharedPrefs.edit().putInt("best_score", currentScore).apply()
        }
        
        // Save overall stats
        val totalPlayed = sharedPrefs.getInt("total_played", 0) + viewModel.totalQuestionsAnswered
        val totalCorrect = sharedPrefs.getInt("total_correct", 0) + viewModel.totalCorrectAnswers
        
        sharedPrefs.edit()
            .putInt("total_played", totalPlayed)
            .putInt("total_correct", totalCorrect)
            .apply()
        
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
