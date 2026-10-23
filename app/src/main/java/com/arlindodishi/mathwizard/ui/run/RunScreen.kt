package com.arlindodishi.mathwizard.ui.run

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arlindodishi.mathwizard.GameFeedback
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.Sfx
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.Balance
import com.arlindodishi.mathwizard.engine.Phase
import com.arlindodishi.mathwizard.engine.Problem
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.SegmentedBar
import com.arlindodishi.mathwizard.ui.pixel.SpellEffect
import com.arlindodishi.mathwizard.ui.pixel.SpellKind
import com.arlindodishi.mathwizard.ui.pixel.SpireBackdrop
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.schools.problemText
import com.arlindodishi.mathwizard.ui.theme.PixelShadow
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun RunRoute(mode: RunMode, onExit: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val feedback = context.appContainer.feedback
    val viewModel: RunViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RunViewModel(context.appContainer.meta, context.appContainer.settings.settings, mode) }
        },
    )
    val loaded by viewModel.ui.collectAsStateWithLifecycle()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }

    val ui = loaded ?: run {
        Box(Modifier.fillMaxSize().background(SpireColors.Night))
        return
    }

    val gameOver = ui.run.phase == Phase.GAME_OVER && ui.feedback == null
    BackHandler(enabled = !gameOver) {
        viewModel.pause()
        confirmLeave = true
    }

    if (gameOver) {
        LaunchedEffect(Unit) { feedback.play(Sfx.GAME_OVER) }
        SummaryScreen(ui = ui, onAgain = viewModel::restart.takeUnless { ui.daily }, onHome = onExit)
    } else {
        RunScreen(
            ui = ui,
            feedback = feedback,
            onChoose = viewModel::choose,
            onHint = viewModel::useHint,
            onContinue = { relicId ->
                feedback.play(if (relicId != null) Sfx.RELIC else Sfx.CLICK)
                viewModel.continueToNextFloor(relicId)
            },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false; viewModel.resume() },
            containerColor = SpireColors.Surface,
            title = { Text(stringResource(R.string.run_leave_title)) },
            text = { Text(stringResource(R.string.run_leave_body)) },
            confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.run_leave_confirm), color = SpireColors.Danger) } },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false; viewModel.resume() }) {
                    Text(stringResource(R.string.run_leave_cancel), color = SpireColors.Text)
                }
            },
        )
    }
}

@Composable
fun RunScreen(
    ui: RunUiState,
    feedback: GameFeedback,
    onChoose: (String) -> Unit,
    onHint: () -> Unit,
    onContinue: (String?) -> Unit,
) {
    val run = ui.run
    val answer = ui.feedback
    val accent = run.problem.school.accent()
    val haptics = LocalHapticFeedback.current

    // Hit: a bolt flies to the enemy, which flashes and shakes. Miss: the screen shakes red.
    val enemyShake = remember { Animatable(0f) }
    val enemyFlash = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }
    val hurtFlash = remember { Animatable(0f) }
    val spell = when (val outcome = answer?.outcome) {
        is AnswerOutcome.Hit -> if (outcome.fast) SpellKind.CRITICAL else SpellKind.HIT
        is AnswerOutcome.Miss -> if (outcome.shielded) null else SpellKind.HURT
        null -> null
    }
    LaunchedEffect(ui.answerCount) {
        when (val outcome = answer?.outcome) {
            is AnswerOutcome.Hit -> {
                feedback.play(if (outcome.fast) Sfx.CRITICAL else Sfx.HIT)
                if (feedback.hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(BOLT_MILLIS)
                enemyFlash.snapTo(1f)
                shake(enemyShake, 14f)
                enemyFlash.animateTo(0f, tween(160))
                if (outcome.enemyDefeated) feedback.play(Sfx.FLOOR)
            }
            is AnswerOutcome.Miss -> {
                feedback.play(if (outcome.shielded) Sfx.SHIELD else Sfx.MISS)
                if (outcome.shielded) return@LaunchedEffect
                if (feedback.hapticsOn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                hurtFlash.snapTo(0.35f)
                shake(screenShake, 12f)
                hurtFlash.animateTo(0f, tween(400))
            }
            null -> Unit
        }
    }

    // Floor change: the pillars scroll (climbing) and a banner announces the new floor.
    val climb by animateFloatAsState(run.floor.toFloat(), tween(900), label = "climb")
    var bannerFloor by remember { mutableIntStateOf(0) }
    LaunchedEffect(run.floor) {
        if (run.isBossFloor) feedback.play(Sfx.BOSS)
        bannerFloor = run.floor
        delay(1300)
        bannerFloor = 0
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SpireColors.Night)
            .offset { IntOffset(screenShake.value.roundToInt(), 0) },
    ) {
        SpireBackdrop(climb = climb, torch = accent, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
        ) {
            TopBar(run.floor, run.isBossFloor, run.playerHp, run.playerMaxHp, ui.timeLeftMillis, run.timeLimitMillis)
            RelicRow(run.relics.map { it.id })
            EnemyPanel(ui, enemyShake.value, enemyFlash.value, Modifier.weight(1f).fillMaxWidth())
            ProblemCard(
                problem = answer?.outcome?.problem ?: run.problem,
                feedback = answer,
                accent = accent,
                hintsLeft = if (answer == null && run.problem.choices.size > 2) run.hintsLeft else 0,
                onChoose = onChoose,
                onHint = onHint,
            )
        }

        SpellEffect(
            trigger = ui.answerCount,
            kind = spell,
            color = if (spell == SpellKind.HURT) SpireColors.Danger else accent,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = hurtFlash.value }
                .background(SpireColors.Danger),
        )

        FloorBanner(visible = bannerFloor != 0 && run.phase == Phase.FIGHTING, floor = run.floor, boss = run.isBossFloor)

        AnimatedVisibility(
            visible = run.phase == Phase.FLOOR_CLEARED && answer == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RelicChoiceOverlay(
                bossCleared = run.isBossFloor,
                floor = run.floor,
                offer = run.relicOffer.map { it.id },
                onPick = onContinue,
            )
        }
    }
}

/** Time for the spell bolt to travel from the card to the enemy. */
private const val BOLT_MILLIS = 280L

private suspend fun shake(anim: Animatable<Float, *>, amplitude: Float) {
    for (x in listOf(amplitude, -amplitude, amplitude * 0.6f, -amplitude * 0.6f, 0f)) {
        anim.animateTo(x, tween(40))
    }
}

@Composable
private fun TopBar(floor: Int, boss: Boolean, hp: Int, maxHp: Int, timeLeft: Long, timeLimit: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (boss) R.string.run_floor_boss else R.string.run_floor, floor).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (boss) SpireColors.Gold else SpireColors.Text,
                modifier = Modifier.weight(1f),
            )
            PixelImage(Sprites.heart, Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.run_hp, hp, maxHp), style = MaterialTheme.typography.titleLarge, color = SpireColors.PlayerHp)
        }
        SegmentedBar(hp.toFloat() / maxHp, SpireColors.PlayerHp, Modifier.fillMaxWidth(), segments = 10, height = 10.dp)
        val timeFraction = (timeLeft.toFloat() / timeLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
        SegmentedBar(
            timeFraction,
            if (timeFraction < 0.3f) SpireColors.Danger else SpireColors.Gold,
            Modifier.fillMaxWidth(),
            segments = 20,
            height = 5.dp,
        )
    }
}

/** Held relics as small framed icons under the top bar, wrapping onto a second row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelicRow(relicIds: List<String>) {
    if (relicIds.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = Spacing.s),
    ) {
        relicIds.forEach { id ->
            val name = stringResource(relicUi(id).name)
            Box(
                Modifier
                    .size(30.dp)
                    .background(SpireColors.Ink)
                    .padding(2.dp)
                    .background(SpireColors.SurfaceRaised)
                    .padding(3.dp)
                    .semantics { contentDescription = name },
            ) {
                PixelImage(Sprites.relic(id), Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EnemyPanel(ui: RunUiState, shakeX: Float, flash: Float, modifier: Modifier) {
    val enemy = ui.run.enemy
    val boss = enemy.type.boss
    // Idle: a two-step bob, like a sprite's two-frame animation.
    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "bob")
    val defeatedAlpha by animateFloatAsState(if (enemy.defeated) 0f else 1f, tween(500), label = "defeat")
    val name = stringResource(enemyNameRes(enemy.type.id))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            PixelImage(
                sprite = Sprites.enemy(enemy.type.id),
                flash = flash,
                alpha = defeatedAlpha,
                modifier = Modifier
                    .size(if (boss) 232.dp else 176.dp)
                    .offset { IntOffset(shakeX.roundToInt(), if (bob < 0.5f) 0 else -4.dp.roundToPx()) }
                    .semantics { contentDescription = name },
            )
            DamagePopup(ui)
        }
        if (boss) {
            Text(stringResource(R.string.run_boss).uppercase(), style = MaterialTheme.typography.labelLarge, color = SpireColors.Gold)
        }
        Text(name.uppercase(), style = MaterialTheme.typography.titleLarge.copy(shadow = PixelShadow), color = SpireColors.Text)
        Spacer(Modifier.height(6.dp))
        SegmentedBar(enemy.hp.toFloat() / enemy.maxHp, SpireColors.EnemyHp, Modifier.fillMaxWidth(0.7f), segments = 10, height = 8.dp)
        Text(
            stringResource(R.string.run_hp, enemy.hp, enemy.maxHp),
            style = MaterialTheme.typography.labelMedium,
            color = SpireColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
        val streakCombo = Balance.comboMultiplier(ui.run.streak, ui.comboShift)
        Text(
            text = if (streakCombo > 1.0) stringResource(R.string.run_combo, streakCombo.toString()).uppercase() else "",
            style = MaterialTheme.typography.labelLarge,
            color = SpireColors.Gold,
        )
    }
}

/** Floating number over the enemy (damage dealt) or a short label (damage taken / timeout). */
@Composable
private fun BoxScope.DamagePopup(ui: RunUiState) {
    val outcome = ui.feedback?.outcome ?: return
    val rise = remember(ui.answerCount) { Animatable(0f) }
    val hit = outcome is AnswerOutcome.Hit
    LaunchedEffect(ui.answerCount) {
        if (hit) delay(BOLT_MILLIS)
        rise.animateTo(1f, tween((RunViewModel.FEEDBACK_MILLIS - BOLT_MILLIS).toInt()))
    }
    val (text, color) = when (outcome) {
        is AnswerOutcome.Hit -> "-${outcome.damage}" to if (outcome.fast) SpireColors.Gold else SpireColors.Text
        is AnswerOutcome.Miss -> if (outcome.shielded) "0" to SpireColors.PlayerHp else "-${outcome.damageTaken} HP" to SpireColors.Danger
    }
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, (-24.dp.toPx() - 40.dp.toPx() * rise.value).roundToInt()) }
            .graphicsLayer { alpha = if (hit && rise.value == 0f) 0f else 1f - rise.value * 0.6f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.displayMedium, color = color)
        val labels = buildList {
            if (outcome is AnswerOutcome.Hit && outcome.fast) add(stringResource(R.string.run_fast) to color)
            if (outcome is AnswerOutcome.Hit && outcome.healed > 0) {
                add(stringResource(R.string.run_healed, outcome.healed) to SpireColors.PlayerHp)
            }
            if (outcome is AnswerOutcome.Miss && outcome.timedOut) add(stringResource(R.string.run_time_up) to color)
            if (outcome is AnswerOutcome.Miss && outcome.shielded) add(stringResource(R.string.run_shielded) to color)
            if (outcome is AnswerOutcome.Miss && outcome.revived) add(stringResource(R.string.run_revived) to SpireColors.Gold)
        }
        labels.forEach { (label, labelColor) ->
            Text(label.uppercase(), style = MaterialTheme.typography.labelLarge.copy(shadow = PixelShadow), color = labelColor)
        }
    }
}

@Composable
private fun ProblemCard(
    problem: Problem,
    feedback: Feedback?,
    accent: Color,
    hintsLeft: Int,
    onChoose: (String) -> Unit,
    onHint: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
        Column(
            Modifier
                .fillMaxWidth()
                .pixelPanel(accent)
                .padding(horizontal = Spacing.s, vertical = Spacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val text = problemText(problem)
            Text(
                text,
                // Word problems and long sequences get a smaller size so they fit on one or two lines.
                style = when {
                    text.length > 26 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp, lineHeight = 30.sp)
                    text.length > 14 -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.displayMedium
                },
                color = SpireColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.m, bottom = Spacing.xs, start = Spacing.l, end = Spacing.l),
            )
            val missed = feedback?.outcome is AnswerOutcome.Miss
            Text(
                text = if (missed) stringResource(R.string.run_answer_was, problem.answer) else "",
                style = MaterialTheme.typography.labelLarge,
                color = SpireColors.Success,
            )
            Spacer(Modifier.height(Spacing.xs))
            problem.choices.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                    row.forEach { choice -> ChoiceButton(choice, problem, feedback, accent, onChoose, Modifier.weight(1f)) }
                }
            }
        }
        if (hintsLeft > 0) {
            val description = pluralStringResource(R.plurals.run_hint_cd, hintsLeft, hintsLeft)
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button, onClick = onHint)
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PixelImage(Sprites.hint, Modifier.size(20.dp))
                Text("$hintsLeft", style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    choice: String,
    problem: Problem,
    feedback: Feedback?,
    accent: Color,
    onChoose: (String) -> Unit,
    modifier: Modifier,
) {
    val color = when {
        feedback == null -> accent
        choice == problem.answer -> SpireColors.Success
        choice == feedback.chosen -> SpireColors.Danger
        else -> SpireColors.Track
    }
    val description = stringResource(R.string.run_answer_cd, choice)
    PixelButton(
        onClick = { onChoose(choice) },
        color = color,
        modifier = modifier
            .height(64.dp)
            .semantics { contentDescription = description },
    ) {
        Text(choice, style = MaterialTheme.typography.headlineMedium.copy(shadow = null), color = Color(0xFF1A0D10))
    }
}

/** Slides in "FLOOR 8" (or the boss warning) when a new floor starts. */
@Composable
private fun FloorBanner(visible: Boolean, floor: Int, boss: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier.fillMaxWidth().padding(top = 140.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .pixelPanel(if (boss) SpireColors.Danger else SpireColors.Gold, fill = SpireColors.Night)
                    .padding(horizontal = Spacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.run_floor, floor).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (boss) SpireColors.Danger else SpireColors.Gold,
                )
                if (boss) {
                    Text(stringResource(R.string.run_boss).uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
                }
            }
        }
    }
}

@Composable
private fun RelicChoiceOverlay(bossCleared: Boolean, floor: Int, offer: List<String>, onPick: (String?) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SpireColors.Night.copy(alpha = 0.93f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.l)) {
            Text(
                (if (bossCleared) stringResource(R.string.boss_cleared_title) else stringResource(R.string.floor_cleared_title, floor)).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = SpireColors.Gold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                if (offer.isEmpty()) stringResource(R.string.relic_none) else stringResource(R.string.relic_choose_title),
                style = MaterialTheme.typography.titleMedium,
                color = SpireColors.TextMuted,
            )
            Spacer(Modifier.height(Spacing.l))
            offer.forEach { id ->
                RelicCard(id, onClick = { onPick(id) })
                Spacer(Modifier.height(Spacing.s))
            }
            if (offer.isEmpty()) {
                PixelButton(onClick = { onPick(null) }, color = SpireColors.Gold, modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.floor_continue).uppercase(), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
private fun RelicCard(id: String, onClick: () -> Unit) {
    val relic = relicUi(id)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .pixelPanel(SpireColors.Gold)
            .padding(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelImage(Sprites.relic(id), Modifier.size(44.dp))
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(stringResource(relic.name), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
            Text(stringResource(relic.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
        }
    }
}

fun enemyNameRes(id: String): Int = when (id) {
    "ember_imp" -> R.string.enemy_ember_imp
    "cinder_slime" -> R.string.enemy_cinder_slime
    "ash_goblin" -> R.string.enemy_ash_goblin
    "flame_wisp" -> R.string.enemy_flame_wisp
    "magma_golem" -> R.string.enemy_magma_golem
    "blaze_hound" -> R.string.enemy_blaze_hound
    "inferno_drake" -> R.string.enemy_inferno_drake
    "tide_sprite" -> R.string.enemy_tide_sprite
    "frost_eel" -> R.string.enemy_frost_eel
    "mist_crab" -> R.string.enemy_mist_crab
    "abyssal_leviathan" -> R.string.enemy_abyssal_leviathan
    "spark_bat" -> R.string.enemy_spark_bat
    "storm_beetle" -> R.string.enemy_storm_beetle
    "volt_serpent" -> R.string.enemy_volt_serpent
    "thunder_titan" -> R.string.enemy_thunder_titan
    "burrow_mole" -> R.string.enemy_burrow_mole
    "stone_tortoise" -> R.string.enemy_stone_tortoise
    "thorn_beast" -> R.string.enemy_thorn_beast
    "mountain_giant" -> R.string.enemy_mountain_giant
    "prism_moth" -> R.string.enemy_prism_moth
    "halo_wisp" -> R.string.enemy_halo_wisp
    "mirror_knight" -> R.string.enemy_mirror_knight
    "solar_sphinx" -> R.string.enemy_solar_sphinx
    "shade_cat" -> R.string.enemy_shade_cat
    "echo_wraith" -> R.string.enemy_echo_wraith
    "void_spider" -> R.string.enemy_void_spider
    "night_lich" -> R.string.enemy_night_lich
    else -> R.string.monster_label
}
