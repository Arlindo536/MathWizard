package com.arlindodishi.mathwizard.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arlindodishi.mathwizard.DifficultyActivity
import com.arlindodishi.mathwizard.HighScoreActivity
import com.arlindodishi.mathwizard.SettingsActivity
import com.arlindodishi.mathwizard.Sfx
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.home.HomeScreen
import com.arlindodishi.mathwizard.ui.run.RunRoute
import com.arlindodishi.mathwizard.ui.schools.SchoolSelectScreen
import com.arlindodishi.mathwizard.ui.theme.MathWizardTheme
import com.arlindodishi.mathwizard.ui.upgrades.UpgradesRoute

private object Routes {
    const val HOME = "home"
    const val UPGRADES = "upgrades"
    const val SCHOOLS = "schools"
    const val RUN_PATTERN = "run/{school}"

    fun run(school: School) = "run/${school.name}"
}

@Composable
fun MathWizardApp() {
    MathWizardTheme {
        val nav = rememberNavController()
        val context = LocalContext.current
        val meta = context.appContainer.meta
        val feedback = context.appContainer.feedback
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                val records by meta.records.collectAsStateWithLifecycle(Records())
                val profile by meta.profile.collectAsStateWithLifecycle(MetaProfile())
                val onlyArithmetic = profile.unlockedSchools == listOf(School.ARITHMETIC)
                val soundOn by feedback.soundOn.collectAsStateWithLifecycle()
                val musicOn by feedback.musicOn.collectAsStateWithLifecycle()
                HomeScreen(
                    bestFloor = records.bestFloor,
                    runsPlayed = records.runsPlayed,
                    crystals = profile.crystals,
                    soundOn = soundOn,
                    musicOn = musicOn,
                    onClimb = { nav.navigate(if (onlyArithmetic) Routes.run(School.ARITHMETIC) else Routes.SCHOOLS) },
                    onUpgrades = { nav.navigate(Routes.UPGRADES) },
                    onClassic = { context.startActivity(Intent(context, DifficultyActivity::class.java)) },
                    onStats = { context.startActivity(Intent(context, HighScoreActivity::class.java)) },
                    onSettings = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    onToggleSound = {
                        feedback.setSound(!soundOn)
                        feedback.play(Sfx.CLICK)
                    },
                    onToggleMusic = { feedback.setMusic(!musicOn) },
                )
            }
            composable(Routes.SCHOOLS) {
                val profile by meta.profile.collectAsStateWithLifecycle(MetaProfile())
                val best by meta.bestBySchool.collectAsStateWithLifecycle(emptyMap())
                SchoolSelectScreen(
                    profile = profile,
                    bestBySchool = best,
                    onPick = { nav.navigate(Routes.run(it)) },
                    onLocked = { nav.navigate(Routes.UPGRADES) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.RUN_PATTERN,
                arguments = listOf(navArgument("school") { type = NavType.StringType }),
            ) { entry ->
                val school = School.valueOf(entry.arguments?.getString("school") ?: School.ARITHMETIC.name)
                RunRoute(school = school, onExit = { nav.popBackStack(Routes.HOME, inclusive = false) })
            }
            composable(Routes.UPGRADES) {
                UpgradesRoute(onBack = { nav.popBackStack() })
            }
        }
    }
}
