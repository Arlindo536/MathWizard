package com.arlindodishi.mathwizard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.arlindodishi.mathwizard.ui.MathWizardApp

/** AppCompatActivity so the in-app language (AppCompatDelegate.setApplicationLocales) applies. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MathWizardApp() }
    }

    override fun onStart() {
        super.onStart()
        appContainer.feedback.onForeground()
    }

    override fun onStop() {
        appContainer.feedback.onBackground()
        super.onStop()
    }
}
