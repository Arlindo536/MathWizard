package com.arlindodishi.mathwizard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.Phase
import com.arlindodishi.mathwizard.data.DailyResult
import com.arlindodishi.mathwizard.data.RecentRun
import com.arlindodishi.mathwizard.data.Records
import com.arlindodishi.mathwizard.data.Settings
import com.arlindodishi.mathwizard.data.Totals
import com.arlindodishi.mathwizard.engine.DailyRift
import com.arlindodishi.mathwizard.engine.PracticeLevel
import com.arlindodishi.mathwizard.engine.Problems
import com.arlindodishi.mathwizard.ui.daily.DailyScreen
import com.arlindodishi.mathwizard.ui.practice.PracticeScreen
import com.arlindodishi.mathwizard.ui.practice.PracticeUiState
import com.arlindodishi.mathwizard.ui.settings.SettingsScreen
import com.arlindodishi.mathwizard.ui.stats.StatsScreen
import com.arlindodishi.mathwizard.ui.stats.StatsUi
import com.arlindodishi.mathwizard.engine.RunConfig
import com.arlindodishi.mathwizard.engine.RunEngine
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.engine.RunState
import com.arlindodishi.mathwizard.data.RunReward
import com.arlindodishi.mathwizard.ui.home.HomeScreen
import com.arlindodishi.mathwizard.ui.run.Feedback
import com.arlindodishi.mathwizard.ui.run.RunScreen
import com.arlindodishi.mathwizard.ui.run.RunUiState
import com.arlindodishi.mathwizard.ui.run.SummaryScreen
import com.arlindodishi.mathwizard.ui.schools.SchoolSelectScreen
import com.arlindodishi.mathwizard.ui.theme.MathWizardTheme
import com.arlindodishi.mathwizard.ui.upgrades.UpgradesScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens on the JVM. Record with `./gradlew recordRoborazziDebug`;
 * images land in docs/screenshots (also used by the README).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val feedback by lazy { ApplicationProvider.getApplicationContext<android.content.Context>().appContainer.feedback }

    private fun capture(name: String, advanceMillis: Long = 300, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MathWizardTheme { content() }
            }
        }
        compose.mainClock.advanceTimeBy(advanceMillis)
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    /** A run on [floor] with a few relics, advanced by answering correctly. */
    private fun runOn(floor: Int, school: School = School.ARITHMETIC, seed: Long = 21L): RunEngine {
        val engine = RunEngine(RunConfig(seed = seed, school = school))
        while (engine.state.floor < floor) {
            while (engine.state.phase == Phase.FIGHTING) engine.answer(engine.state.problem.answer, engine.state.timeLimitMillis / 2)
            engine.nextFloor(engine.state.relicOffer.firstOrNull()?.id)
        }
        return engine
    }

    private fun ui(state: RunState, feedback: Feedback? = null, answers: Int = 0) =
        RunUiState(run = state, timeLeftMillis = state.timeLimitMillis * 6 / 10, feedback = feedback, answerCount = answers)

    @Test
    fun home() = capture("01_home") {
        HomeScreen(
            bestFloor = 17, runsPlayed = 23, crystals = 245, soundOn = true, musicOn = true, dailyDone = false, dailyStreak = 4,
            onClimb = {}, onDaily = {}, onPractice = {}, onUpgrades = {}, onClassic = {}, onStats = {}, onSettings = {},
            onToggleSound = {}, onToggleMusic = {},
        )
    }

    @Test
    fun run() {
        val engine = runOn(7)
        engine.answer(engine.state.problem.answer, 1_000)
        capture("02_run") { RunScreen(ui(engine.state), feedback, {}, {}, {}) }
    }

    @Test
    fun spellHit() {
        val engine = runOn(3, School.FRACTIONS, seed = 5L)
        val outcome = engine.answer(engine.state.problem.answer, 800) as AnswerOutcome.Hit
        capture("03_spell", advanceMillis = 200) { RunScreen(ui(engine.state, Feedback(outcome, outcome.problem.answer), answers = 1), feedback, {}, {}, {}) }
    }

    @Test
    fun boss() {
        val engine = runOn(10, School.ALGEBRA, seed = 9L)
        capture("04_boss", advanceMillis = 1600) { RunScreen(ui(engine.state), feedback, {}, {}, {}) }
    }

    @Test
    fun relicChoice() {
        val engine = runOn(4)
        while (engine.state.phase == Phase.FIGHTING) engine.answer(engine.state.problem.answer, 1_000)
        capture("05_relics", advanceMillis = 600) { RunScreen(ui(engine.state), feedback, {}, {}, {}) }
    }

    @Test
    @Config(qualifiers = "sq-w400dp-h860dp-xxhdpi")
    fun geometryInAlbanian() {
        val engine = runOn(14, School.GEOMETRY, seed = 3L)
        capture("06_geometry_sq", advanceMillis = 1600) { RunScreen(ui(engine.state), feedback, {}, {}, {}) }
    }

    @Test
    fun summary() {
        val engine = runOn(12)
        while (engine.state.phase != Phase.GAME_OVER) {
            if (engine.state.phase == Phase.FLOOR_CLEARED) engine.nextFloor()
            engine.answer(engine.state.problem.choices.first { it != engine.state.problem.answer }, 1_000)
        }
        val state = ui(engine.state).copy(reward = RunReward(crystals = 61, newRecord = true, bestFloor = engine.state.floor))
        capture("07_summary") { SummaryScreen(state, {}, {}) }
    }

    @Test
    fun upgrades() = capture("08_upgrades") {
        UpgradesScreen(
            profile = MetaProfile(crystals = 245, levels = mapOf("school_fractions" to 1, "school_algebra" to 1, "vitality" to 2, "focus" to 1)),
            onBuy = {}, onBack = {},
        )
    }

    @Test
    fun schools() = capture("09_schools") {
        SchoolSelectScreen(
            profile = MetaProfile(levels = mapOf("school_fractions" to 1, "school_algebra" to 1, "school_powers" to 1)),
            bestBySchool = mapOf(School.ARITHMETIC to 17, School.FRACTIONS to 9, School.ALGEBRA to 4),
            onPick = {}, onLocked = {}, onBack = {},
        )
    }

    @Test
    fun runWithLargeText() {
        val engine = runOn(14, School.GEOMETRY, seed = 3L)
        capture("a11y_run_text130", advanceMillis = 1600, fontScale = 1.3f) { RunScreen(ui(engine.state), feedback, {}, {}, {}) }
    }

    @Test
    fun homeWithLargeText() {
        capture("a11y_home_text200", fontScale = 2.0f) {
            HomeScreen(
                bestFloor = 17, runsPlayed = 23, crystals = 245, soundOn = true, musicOn = false, dailyDone = true, dailyStreak = 4,
                onClimb = {}, onDaily = {}, onPractice = {}, onUpgrades = {}, onClassic = {}, onStats = {}, onSettings = {},
                onToggleSound = {}, onToggleMusic = {},
            )
        }
    }

    /**
     * Frames for the README demo GIF (build/demo/frame_NNN.png, assembled by
     * tools/make_demo_gif.py): a hit with its spell, a miss, then clearing the floor.
     */
    @Test
    fun demoFrames() {
        val engine = runOn(2, seed = 8L)
        var state by mutableStateOf(ui(engine.state))
        compose.mainClock.autoAdvance = false
        compose.setContent { MathWizardTheme { RunScreen(state, feedback, {}, {}, {}) } }
        var frame = 0
        fun shoot(times: Int, stepMillis: Long) = repeat(times) {
            compose.mainClock.advanceTimeBy(stepMillis)
            compose.onRoot().captureRoboImage("build/demo/frame_%03d.png".format(frame++))
        }
        fun answer(correct: Boolean) {
            val p = engine.state.problem
            val chosen = if (correct) p.answer else p.choices.first { it != p.answer }
            val outcome = engine.answer(chosen, if (correct) 1_500 else 4_000)
            state = state.copy(run = engine.state, feedback = Feedback(outcome, chosen), answerCount = state.answerCount + 1)
        }
        fun settle() {
            state = state.copy(feedback = null, timeLeftMillis = engine.state.timeLimitMillis)
        }
        shoot(8, 150) // banner leaves, idle bob
        answer(correct = true); shoot(10, 70); settle(); shoot(5, 150)
        answer(correct = true); shoot(10, 70); settle(); shoot(4, 150)
        answer(correct = false); shoot(8, 80); settle(); shoot(4, 150)
        while (engine.state.phase == Phase.FIGHTING) {
            answer(correct = true); shoot(10, 70)
            if (engine.state.phase == Phase.FIGHTING) { settle(); shoot(3, 150) }
        }
        settle(); shoot(10, 150) // relic choice
    }

    @Test
    fun daily() = capture("10_daily") {
        DailyScreen(
            today = 20_386,
            results = listOf(20_385L, 20_384L, 20_383L).mapIndexed { i, d -> DailyResult(d, DailyRift.school(d), 9 - i, 30, 34, 40) },
            onStart = {}, onBack = {},
        )
    }

    @Test
    fun practice() {
        val problem = Problems.generate(School.POWERS, PracticeLevel.HARD.difficulty, 4L)
        capture("11_practice") {
            PracticeScreen(School.POWERS, PracticeLevel.HARD, PracticeUiState(problem, correct = 7, answered = 8, streak = 5), {}, {}, {})
        }
    }

    @Test
    fun stats() = capture("12_stats") {
        StatsScreen(
            StatsUi(
                records = Records(runsPlayed = 23, bestFloor = 17, totalCrystals = 812),
                totals = Totals(answered = 640, correct = 551, bosses = 14, bestStreak = 31),
                bestBySchool = mapOf(School.ARITHMETIC to 17, School.FRACTIONS to 9, School.ALGEBRA to 4),
                recent = listOf(RecentRun(School.FRACTIONS, 9, 1_792_700_000_000, 41), RecentRun(School.ARITHMETIC, 17, 1_792_600_000_000, 77)),
                dailyStreak = 4, bestDailyStreak = 6,
            ),
            onBack = {},
        )
    }

    @Test
    fun settings() = capture("13_settings") {
        SettingsScreen(Settings(relaxed = true), language = "sq", version = "2.0-alpha8", onToggle = { _, _ -> }, onLanguage = {}, onBack = {})
    }
}
