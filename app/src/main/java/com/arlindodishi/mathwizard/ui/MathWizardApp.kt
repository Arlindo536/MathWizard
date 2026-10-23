package com.arlindodishi.mathwizard.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arlindodishi.mathwizard.DifficultyActivity
import com.arlindodishi.mathwizard.HighScoreActivity
import com.arlindodishi.mathwizard.SettingsActivity
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.ui.home.HomeScreen
import com.arlindodishi.mathwizard.ui.run.RunRoute
import com.arlindodishi.mathwizard.ui.theme.MathWizardTheme
import com.arlindodishi.mathwizard.ui.upgrades.UpgradesRoute

private object Routes {
    const val HOME = "home"
    const val RUN = "run"
    const val UPGRADES = "upgrades"
}

@Composable
fun MathWizardApp() {
    MathWizardTheme {
        val nav = rememberNavController()
        val context = LocalContext.current
        val meta = context.appContainer.meta
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                val records by meta.records.collectAsStateWithLifecycle(Records())
                val profile by meta.profile.collectAsStateWithLifecycle(MetaProfile())
                HomeScreen(
                    bestFloor = records.bestFloor,
                    runsPlayed = records.runsPlayed,
                    crystals = profile.crystals,
                    onClimb = { nav.navigate(Routes.RUN) },
                    onUpgrades = { nav.navigate(Routes.UPGRADES) },
                    onClassic = { context.startActivity(Intent(context, DifficultyActivity::class.java)) },
                    onStats = { context.startActivity(Intent(context, HighScoreActivity::class.java)) },
                    onSettings = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                )
            }
            composable(Routes.RUN) {
                RunRoute(onExit = { nav.popBackStack(Routes.HOME, inclusive = false) })
            }
            composable(Routes.UPGRADES) {
                UpgradesRoute(onBack = { nav.popBackStack() })
            }
        }
    }
}
