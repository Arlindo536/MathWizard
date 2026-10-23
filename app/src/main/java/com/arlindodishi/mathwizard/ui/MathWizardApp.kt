package com.arlindodishi.mathwizard.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arlindodishi.mathwizard.DifficultyActivity
import com.arlindodishi.mathwizard.Sfx
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.PracticeLevel
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.Streaks
import com.arlindodishi.mathwizard.epochDayNow
import com.arlindodishi.mathwizard.ui.daily.DailyScreen
import com.arlindodishi.mathwizard.ui.home.HomeScreen
import com.arlindodishi.mathwizard.ui.practice.PracticeRoute
import com.arlindodishi.mathwizard.ui.practice.PracticeSetupScreen
import com.arlindodishi.mathwizard.ui.run.RunMode
import com.arlindodishi.mathwizard.ui.run.RunRoute
import com.arlindodishi.mathwizard.ui.schools.SchoolSelectScreen
import com.arlindodishi.mathwizard.ui.settings.SettingsRoute
import com.arlindodishi.mathwizard.ui.stats.StatsRoute
import com.arlindodishi.mathwizard.ui.theme.MathWizardTheme
import com.arlindodishi.mathwizard.ui.upgrades.UpgradesRoute

private object Routes {
    const val HOME = "home"
    const val UPGRADES = "upgrades"
    const val SCHOOLS = "schools"
    const val DAILY = "daily"
    const val PRACTICE = "practice"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val RUN_PATTERN = "run/{school}"
    const val DAILY_RUN_PATTERN = "daily/{day}"
    const val PRACTICE_RUN_PATTERN = "practice/{school}/{level}"

    fun run(school: School) = "run/${school.name}"
    fun dailyRun(day: Long) = "daily/$day"
    fun practiceRun(school: School, level: PracticeLevel) = "practice/${school.name}/${level.name}"
}

@Composable
fun MathWizardApp() {
    MathWizardTheme {
        val nav = rememberNavController()
        val context = LocalContext.current
        val meta = context.appContainer.meta
        val feedback = context.appContainer.feedback
        val goHome: () -> Unit = { nav.popBackStack(Routes.HOME, inclusive = false) }
        NavHost(navController = nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                val records by meta.records.collectAsStateWithLifecycle(Records())
                val profile by meta.profile.collectAsStateWithLifecycle(MetaProfile())
                val daily by meta.dailyResults.collectAsStateWithLifecycle(emptyList())
                val soundOn by feedback.soundOn.collectAsStateWithLifecycle()
                val musicOn by feedback.musicOn.collectAsStateWithLifecycle()
                val today = remember { epochDayNow() }
                val onlyArithmetic = profile.unlockedSchools == listOf(School.ARITHMETIC)
                HomeScreen(
                    bestFloor = records.bestFloor,
                    runsPlayed = records.runsPlayed,
                    crystals = profile.crystals,
                    soundOn = soundOn,
                    musicOn = musicOn,
                    dailyDone = daily.any { it.epochDay == today },
                    dailyStreak = Streaks.current(daily.map { it.epochDay }.toSet(), today),
                    onClimb = { nav.navigate(if (onlyArithmetic) Routes.run(School.ARITHMETIC) else Routes.SCHOOLS) },
                    onDaily = { nav.navigate(Routes.DAILY) },
                    onPractice = { nav.navigate(Routes.PRACTICE) },
                    onUpgrades = { nav.navigate(Routes.UPGRADES) },
                    onClassic = { context.startActivity(Intent(context, DifficultyActivity::class.java)) },
                    onStats = { nav.navigate(Routes.STATS) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
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
            composable(Routes.RUN_PATTERN, arguments = listOf(navArgument("school") { type = NavType.StringType })) { entry ->
                val school = School.valueOf(entry.arguments?.getString("school") ?: School.ARITHMETIC.name)
                RunRoute(mode = RunMode.Spire(school), onExit = goHome)
            }
            composable(Routes.DAILY) {
                val daily by meta.dailyResults.collectAsStateWithLifecycle(emptyList())
                val today = remember { epochDayNow() }
                DailyScreen(today = today, results = daily, onStart = { nav.navigate(Routes.dailyRun(today)) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.DAILY_RUN_PATTERN, arguments = listOf(navArgument("day") { type = NavType.LongType })) { entry ->
                val day = entry.arguments?.getLong("day") ?: epochDayNow()
                RunRoute(mode = RunMode.Daily(day), onExit = { nav.popBackStack(Routes.DAILY, inclusive = false) })
            }
            composable(Routes.PRACTICE) {
                val profile by meta.profile.collectAsStateWithLifecycle(MetaProfile())
                PracticeSetupScreen(profile = profile, onStart = { s, l -> nav.navigate(Routes.practiceRun(s, l)) }, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.PRACTICE_RUN_PATTERN,
                arguments = listOf(navArgument("school") { type = NavType.StringType }, navArgument("level") { type = NavType.StringType }),
            ) { entry ->
                val school = School.valueOf(entry.arguments?.getString("school") ?: School.ARITHMETIC.name)
                val level = PracticeLevel.valueOf(entry.arguments?.getString("level") ?: PracticeLevel.EASY.name)
                PracticeRoute(school, level, onBack = { nav.popBackStack() })
            }
            composable(Routes.UPGRADES) { UpgradesRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.STATS) { StatsRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsRoute(onBack = { nav.popBackStack() }) }
        }
    }
}
