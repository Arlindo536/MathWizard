package com.arlindodishi.mathwizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arlindodishi.mathwizard.ui.MathWizardApp

class MainActivity : ComponentActivity() {

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
