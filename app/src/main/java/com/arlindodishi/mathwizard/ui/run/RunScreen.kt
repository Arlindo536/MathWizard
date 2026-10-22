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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
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
fun RunScreen(ui: RunUiState, onChoose: (String) -> Unit, onContinue: (String?) -> Unit) {
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
            TopBar(run.floor, run.isBossFloor, run.playerHp, run.playerMaxHp, ui.timeLeftMillis, run.timeLimitMillis)
            RelicRow(run.relics.map { it.id })
            Spacer(Modifier.height(Spacing.s))
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
            RelicChoiceOverlay(
                bossCleared = run.isBossFloor,
                floor = run.floor,
                offer = run.relicOffer.map { it.id },
                onPick = onContinue,
            )
        }
    }
}

private suspend fun shake(anim: Animatable<Float, *>, amplitude: Float) {
    for (x in listOf(amplitude, -amplitude, amplitude * 0.6f, -amplitude * 0.6f, 0f)) {
        anim.animateTo(x, tween(45))
    }
}

@Composable
private fun TopBar(floor: Int, boss: Boolean, hp: Int, maxHp: Int, timeLeft: Long, timeLimit: Long) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (boss) R.string.run_floor_boss else R.string.run_floor, floor),
                style = MaterialTheme.typography.headlineMedium,
                color = if (boss) SpireColors.Gold else SpireColors.Text,
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

/** Held relics as a row of small icons under the top bar. */
@Composable
private fun RelicRow(relicIds: List<String>) {
    if (relicIds.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.padding(top = Spacing.s),
    ) {
        relicIds.forEach { id ->
            val relic = relicUi(id)
            val name = stringResource(relic.name)
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SpireColors.Surface)
                    .border(1.dp, SpireColors.Outline, CircleShape)
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Text(relic.emoji, style = MaterialTheme.typography.bodyMedium)
            }
        }
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
                accent = if (enemy.type.boss) SpireColors.Gold else accent,
                defeated = enemy.defeated,
                modifier = Modifier
                    .size(if (enemy.type.boss) 220.dp else 180.dp)
                    .offset { IntOffset(shakeX.roundToInt(), 0) },
            )
            DamagePopup(ui)
        }
        Spacer(Modifier.height(Spacing.m))
        if (enemy.type.boss) {
            Text(
                stringResource(R.string.run_boss).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = SpireColors.Gold,
            )
        }
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
        val streakCombo = Balance.comboMultiplier(ui.run.streak, ui.comboShift)
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
        is AnswerOutcome.Miss -> if (outcome.shielded) "0" to SpireColors.PlayerHp else "-${outcome.damageTaken} HP" to SpireColors.Danger
    }
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(0, (-60 * rise.value).roundToInt()) }
            .graphicsLayer { alpha = 1f - rise.value * 0.7f },
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
            Text(label, style = MaterialTheme.typography.labelLarge, color = labelColor)
        }
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
private fun RelicChoiceOverlay(bossCleared: Boolean, floor: Int, offer: List<String>, onPick: (String?) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SpireColors.Night.copy(alpha = 0.92f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(Spacing.l)) {
            Text(
                if (bossCleared) stringResource(R.string.boss_cleared_title) else stringResource(R.string.floor_cleared_title, floor),
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
                Button(onClick = { onPick(null) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.floor_continue), style = MaterialTheme.typography.titleLarge)
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
            .clip(RoundedCornerShape(16.dp))
            .background(SpireColors.Surface)
            .border(1.dp, SpireColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(relic.emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.width(Spacing.m))
        Column {
            Text(stringResource(relic.name), style = MaterialTheme.typography.titleLarge, color = SpireColors.Text)
            Text(stringResource(relic.description), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
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
    "inferno_drake" -> "🐉" // 🐉
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
    "inferno_drake" -> R.string.enemy_inferno_drake
    else -> R.string.monster_label
}
