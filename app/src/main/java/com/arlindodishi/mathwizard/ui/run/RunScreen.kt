package com.arlindodishi.mathwizard.ui.run

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.data.PrefsSpireRecords
import com.arlindodishi.mathwizard.engine.AnswerOutcome
import com.arlindodishi.mathwizard.engine.Balance
import com.arlindodishi.mathwizard.engine.Phase
import com.arlindodishi.mathwizard.engine.Problem
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import kotlin.math.roundToInt

@Composable
fun RunRoute(onExit: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val viewModel: RunViewModel = viewModel(
        factory = viewModelFactory { initializer { RunViewModel(PrefsSpireRecords(context)) } },
    )
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resume() }

    val gameOver = ui.run.phase == Phase.GAME_OVER && ui.feedback == null
    BackHandler(enabled = !gameOver) {
        viewModel.pause()
        confirmLeave = true
    }

    if (gameOver) {
        SummaryScreen(ui = ui, onAgain = viewModel::restart, onHome = onExit)
    } else {
        RunScreen(ui = ui, onChoose = viewModel::choose, onContinue = viewModel::continueToNextFloor)
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false; viewModel.resume() },
            title = { Text(stringResource(R.string.run_leave_title)) },
            text = { Text(stringResource(R.string.run_leave_body)) },
            confirmButton = { TextButton(onClick = onExit) { Text(stringResource(R.string.run_leave_confirm)) } },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false; viewModel.resume() }) {
                    Text(stringResource(R.string.run_leave_cancel))
                }
            },
        )
    }
}

@Composable
fun RunScreen(ui: RunUiState, onChoose: (String) -> Unit, onContinue: () -> Unit) {
    val run = ui.run
    val feedback = ui.feedback
    val accent = run.problem.school.accent()
    val haptics = LocalHapticFeedback.current

    // Enemy shakes when hit, the whole screen nudges and flashes red when the player is hit.
    val enemyShake = remember { Animatable(0f) }
    val screenShake = remember { Animatable(0f) }
    val hurtFlash = remember { Animatable(0f) }
    LaunchedEffect(ui.answerCount) {
        when (feedback?.outcome) {
            is AnswerOutcome.Hit -> {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                shake(enemyShake, 14f)
            }
            is AnswerOutcome.Miss -> {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                hurtFlash.snapTo(0.35f)
                shake(screenShake, 10f)
                hurtFlash.animateTo(0f, tween(400))
            }
            null -> Unit
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpireColors.Night, SpireColors.NightRaised)))
            .offset { IntOffset(screenShake.value.roundToInt(), 0) },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.m, vertical = Spacing.s),
        ) {
            TopBar(run.floor, run.playerHp, run.playerMaxHp, ui.timeLeftMillis, run.timeLimitMillis)
            Spacer(Modifier.height(Spacing.l))
            EnemyPanel(ui, accent, enemyShake.value, Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(Spacing.m))
            ProblemCard(feedback?.outcome?.problem ?: run.problem, feedback, accent, onChoose)
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = hurtFlash.value }
                .background(SpireColors.Danger),
        )

        AnimatedVisibility(
            visible = run.phase == Phase.FLOOR_CLEARED && feedback == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FloorClearedOverlay(run.floor, healed = minOf(Balance.FLOOR_HEAL, run.playerMaxHp - run.playerHp), onContinue)
        }
    }
}

private suspend fun shake(anim: Animatable<Float, *>, amplitude: Float) {
    for (x in listOf(amplitude, -amplitude, amplitude * 0.6f, -amplitude * 0.6f, 0f)) {
        anim.animateTo(x, tween(45))
    }
}

@Composable
private fun TopBar(floor: Int, hp: Int, maxHp: Int, timeLeft: Long, timeLimit: Long) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.run_floor, floor),
                style = MaterialTheme.typography.headlineMedium,
                color = SpireColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.run_hp, hp, maxHp),
                style = MaterialTheme.typography.titleMedium,
                color = SpireColors.PlayerHp,
            )
        }
        Spacer(Modifier.height(Spacing.s))
        Meter(fraction = hp.toFloat() / maxHp, color = SpireColors.PlayerHp, height = 10.dp)
        Spacer(Modifier.height(Spacing.s))
        val timeFraction = (timeLeft.toFloat() / timeLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
        Meter(
            fraction = timeFraction,
            color = if (timeFraction < 0.3f) SpireColors.Danger else SpireColors.Gold,
            height = 6.dp,
            animate = false,
        )
    }
}

@Composable
private fun Meter(fraction: Float, color: Color, height: Dp, animate: Boolean = true) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(if (animate) 350 else 0), label = "meter")
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(SpireColors.Outline),
    ) {
        Box(
            Modifier
                .fillMaxWidth(shown)
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
private fun EnemyPanel(ui: RunUiState, accent: Color, shakeX: Float, modifier: Modifier) {
    val enemy = ui.run.enemy
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(contentAlignment = Alignment.Center) {
            EnemyArt(
                enemyId = enemy.type.id,
                accent = accent,
                defeated = enemy.defeated,
                modifier = Modifier
                    .size(180.dp)
                    .offset { IntOffset(shakeX.roundToInt(), 0) },
            )
            DamagePopup(ui)
        }
        Spacer(Modifier.height(Spacing.m))
        Text(
            stringResource(enemyNameRes(enemy.type.id)),
            style = MaterialTheme.typography.titleLarge,
            color = SpireColors.Text,
        )
        Spacer(Modifier.height(Spacing.s))
        Box(Modifier.fillMaxWidth(0.7f)) {
            Meter(fraction = enemy.hp.toFloat() / enemy.maxHp, color = SpireColors.EnemyHp, height = 8.dp)
        }
        Spacer(Modifier.height(Spacing.s))
        Text(
            stringResource(R.string.run_hp, enemy.hp, enemy.maxHp),
            style = MaterialTheme.typography.labelMedium,
            color = SpireColors.TextMuted,
        )
        val streakCombo = Balance.comboMultiplier(ui.run.streak)
        Text(
            text = if (streakCombo > 1.0) stringResource(R.string.run_combo, streakCombo.toString()) else "",
            style = MaterialTheme.typography.labelLarge,
            color = SpireColors.Gold,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/** Floating number over the enemy (damage dealt) or a short label (damage taken / timeout). */
@Composable
private fun BoxScope.DamagePopup(ui: RunUiState) {
    val outcome = ui.feedback?.outcome ?: return
    val rise = remember(ui.answerCount) { Animatable(0f) }
    LaunchedEffect(ui.answerCount) { rise.animateTo(1f, tween(RunViewModel.FEEDBACK_MILLIS.toInt())) }
    val (text, color) = when (outcome) {
        is AnswerOutcome.Hit -> "-${outcome.damage}" to if (outcome.fast) SpireColors.Gold else SpireColors.Text
        is AnswerOutcome.Miss -> "-${outcome.damageTaken} HP" to SpireColors.Danger
    }
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, (-60 * rise.value).roundToInt()) }
            .graphicsLayer { alpha = 1f - rise.value * 0.7f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.displayMedium, color = color)
        val label = when {
            outcome is AnswerOutcome.Hit && outcome.fast -> stringResource(R.string.run_fast)
            outcome is AnswerOutcome.Miss && outcome.timedOut -> stringResource(R.string.run_time_up)
            else -> null
        }
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun ProblemCard(problem: Problem, feedback: Feedback?, accent: Color, onChoose: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SpireColors.Surface)
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            problem.prompt,
            style = MaterialTheme.typography.displayMedium,
            color = SpireColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = Spacing.m),
        )
        val missed = feedback?.outcome is AnswerOutcome.Miss
        Text(
            text = if (missed) stringResource(R.string.run_answer_was, problem.answer) else "",
            style = MaterialTheme.typography.labelLarge,
            color = SpireColors.Success,
        )
        Spacer(Modifier.height(Spacing.s))
        problem.choices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(vertical = Spacing.xs)) {
                row.forEach { choice ->
                    ChoiceButton(choice, problem, feedback, accent, onChoose, Modifier.weight(1f))
                }
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
    val container = when {
        feedback == null -> accent
        choice == problem.answer -> SpireColors.Success
        choice == feedback.chosen -> SpireColors.Danger
        else -> SpireColors.Outline
    }
    val description = stringResource(R.string.run_answer_cd, choice)
    Button(
        onClick = { onChoose(choice) },
        enabled = feedback == null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = SpireColors.Night,
            disabledContainerColor = container,
            disabledContentColor = SpireColors.Night,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(64.dp)
            .semantics { contentDescription = description },
    ) {
        Text(choice, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun FloorClearedOverlay(floor: Int, healed: Int, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SpireColors.Night.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.xl)) {
            Text(
                stringResource(R.string.floor_cleared_title, floor),
                style = MaterialTheme.typography.displayMedium,
                color = SpireColors.Gold,
                textAlign = TextAlign.Center,
            )
            if (healed > 0) {
                Spacer(Modifier.height(Spacing.s))
                Text(
                    stringResource(R.string.floor_cleared_heal, healed),
                    style = MaterialTheme.typography.titleMedium,
                    color = SpireColors.PlayerHp,
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Button(onClick = onContinue, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.floor_continue), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Placeholder enemy art: an emoji in a glowing orb, replaced in the visual pass (step 7). */
@Composable
fun EnemyArt(enemyId: String, accent: Color, defeated: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(if (defeated) 0.15f else 1f, tween(400), label = "enemyAlpha")
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.08f), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) {
        Text(enemyEmoji(enemyId), style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize * 2))
    }
}

private fun enemyEmoji(id: String): String = when (id) {
    "ember_imp" -> "👺"     // 👺
    "cinder_slime" -> "🦠"  // 🦠
    "ash_goblin" -> "👹"    // 👹
    "flame_wisp" -> "🔥"    // 🔥
    "magma_golem" -> "🪨"   // 🪨
    "blaze_hound" -> "🐺"   // 🐺
    "wizard" -> "🧙"        // 🧙
    else -> "👾"            // 👾
}

fun enemyNameRes(id: String): Int = when (id) {
    "ember_imp" -> R.string.enemy_ember_imp
    "cinder_slime" -> R.string.enemy_cinder_slime
    "ash_goblin" -> R.string.enemy_ash_goblin
    "flame_wisp" -> R.string.enemy_flame_wisp
    "magma_golem" -> R.string.enemy_magma_golem
    "blaze_hound" -> R.string.enemy_blaze_hound
    else -> R.string.monster_label
}
