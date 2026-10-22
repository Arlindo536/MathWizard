package com.arlindodishi.mathwizard.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arlindodishi.mathwizard.DifficultyActivity
import com.arlindodishi.mathwizard.HighScoreActivity
import com.arlindodishi.mathwizard.SettingsActivity
import com.arlindodishi.mathwizard.data.PrefsSpireRecords
import com.arlindodishi.mathwizard.ui.home.HomeScreen
import com.arlindodishi.mathwizard.ui.run.RunRoute
import com.arlindodishi.mathwizard.ui.theme.MathWizardTheme

private object Routes {
    const val HOME = "home"
    const val RUN = "run"
}

@Composable
fun MathWizardApp() {
    MathWizardTheme {
        val nav = rememberNavController()
        val context = LocalContext.current
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                // Re-read on every visit so the best floor updates after a run.
                val records = remember { PrefsSpireRecords(context) }
                HomeScreen(
                    bestFloor = records.bestFloor,
                    runsPlayed = records.runsPlayed,
                    onClimb = { nav.navigate(Routes.RUN) },
                    onClassic = { context.startActivity(Intent(context, DifficultyActivity::class.java)) },
                    onStats = { context.startActivity(Intent(context, HighScoreActivity::class.java)) },
                    onSettings = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                )
            }
            composable(Routes.RUN) {
                RunRoute(onExit = { nav.popBackStack(Routes.HOME, inclusive = false) })
            }
        }
    }
}
