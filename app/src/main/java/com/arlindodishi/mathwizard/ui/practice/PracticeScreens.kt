package com.arlindodishi.mathwizard.ui.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.Sfx
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.engine.MetaProfile
import com.arlindodishi.mathwizard.engine.PracticeLevel
import com.arlindodishi.mathwizard.engine.School
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelImage
import com.arlindodishi.mathwizard.ui.pixel.Sprites
import com.arlindodishi.mathwizard.ui.pixel.pixelPanel
import com.arlindodishi.mathwizard.ui.schools.problemText
import com.arlindodishi.mathwizard.ui.schools.schoolUi
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.theme.accent
import com.arlindodishi.mathwizard.ui.upgrades.ScreenHeader

fun PracticeLevel.labelRes(): Int = when (this) {
    PracticeLevel.EASY -> R.string.practice_level_easy
    PracticeLevel.MEDIUM -> R.string.practice_level_medium
    PracticeLevel.HARD -> R.string.practice_level_hard
    PracticeLevel.EXPERT -> R.string.practice_level_expert
}

/** Pick an unlocked school and a level. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeSetupScreen(profile: MetaProfile, onStart: (School, PracticeLevel) -> Unit, onBack: () -> Unit) {
    var school by rememberSaveable { mutableStateOf(School.ARITHMETIC) }
    var level by rememberSaveable { mutableStateOf(PracticeLevel.EASY) }
    Column(Modifier.fillMaxSize().background(SpireColors.Night).safeDrawingPadding()) {
        ScreenHeader(title = stringResource(R.string.home_practice), onBack = onBack)
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.m),
        ) {
            Text(stringResource(R.string.practice_desc), style = MaterialTheme.typography.bodyLarge, color = SpireColors.TextMuted)
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.practice_school).uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
            Spacer(Modifier.height(Spacing.s))
            profile.unlockedSchools.forEach { s ->
                val selected = s == school
                PixelButton(
                    onClick = { school = s },
                    color = if (selected) s.accent() else SpireColors.SurfaceRaised,
                    contentColor = if (selected) SpireColors.Night else SpireColors.Text,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).semantics { this.selected = selected },
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PixelImage(Sprites.school(s), Modifier.size(24.dp))
                        Spacer(Modifier.width(Spacing.m))
                        Text(stringResource(schoolUi(s).name).uppercase(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.practice_level).uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
            Spacer(Modifier.height(Spacing.s))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PracticeLevel.entries.forEach { l ->
                    val selected = l == level
                    PixelButton(
                        onClick = { level = l },
                        color = if (selected) SpireColors.Gold else SpireColors.SurfaceRaised,
                        contentColor = if (selected) SpireColors.Night else SpireColors.Text,
                        modifier = Modifier.semantics { this.selected = selected },
                    ) {
                        Text(stringResource(l.labelRes()).uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            PixelButton(onClick = { onStart(school, level) }, color = school.accent(), modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(stringResource(R.string.practice_start).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(Spacing.l))
        }
    }
}

@Composable
fun PracticeRoute(school: School, level: PracticeLevel, onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val feedback = context.appContainer.feedback
    val viewModel: PracticeViewModel = viewModel(factory = viewModelFactory { initializer { PracticeViewModel(school, level) } })
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.answered) {
        if (ui.answered > 0) feedback.play(if (ui.wasCorrect) Sfx.HIT else Sfx.MISS)
    }
    PracticeScreen(school, level, ui, viewModel::choose, viewModel::next, onBack)
}

@Composable
fun PracticeScreen(
    school: School,
    level: PracticeLevel,
    ui: PracticeUiState,
    onChoose: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val accent = school.accent()
    Column(Modifier.fillMaxSize().background(SpireColors.Night).safeDrawingPadding()) {
        ScreenHeader(title = stringResource(schoolUi(school).name), onBack = onBack) {
            Text(stringResource(level.labelRes()).uppercase(), style = MaterialTheme.typography.titleMedium, color = SpireColors.Gold)
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.practice_score, ui.correct, ui.answered), style = MaterialTheme.typography.headlineMedium, color = SpireColors.Text)
            Text(
                if (ui.streak >= 2) stringResource(R.string.practice_streak, ui.streak).uppercase() else "",
                style = MaterialTheme.typography.titleLarge,
                color = SpireColors.Gold,
            )
            Spacer(Modifier.weight(1f))
            Column(
                Modifier.fillMaxWidth().pixelPanel(accent).padding(Spacing.s),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = problemText(ui.problem)
                Text(
                    text,
                    style = when {
                        text.length > 26 -> MaterialTheme.typography.headlineMedium.copy(fontSize = 27.sp, lineHeight = 30.sp)
                        text.length > 14 -> MaterialTheme.typography.headlineMedium
                        else -> MaterialTheme.typography.displayMedium
                    },
                    color = SpireColors.Text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = Spacing.m),
                )
                Text(
                    when {
                        !ui.showingAnswer -> ""
                        ui.wasCorrect -> stringResource(R.string.correct_answer).uppercase()
                        else -> stringResource(R.string.run_answer_was, ui.problem.answer)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (ui.wasCorrect) SpireColors.Success else SpireColors.Danger,
                )
                Spacer(Modifier.height(Spacing.s))
                ui.problem.choices.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 5.dp)) {
                        row.forEach { choice ->
                            val color = when {
                                !ui.showingAnswer -> accent
                                ui.problem.isCorrect(choice) -> SpireColors.Success
                                choice == ui.chosen -> SpireColors.Danger
                                else -> SpireColors.Track
                            }
                            val description = stringResource(R.string.run_answer_cd, choice)
                            PixelButton(
                                onClick = { onChoose(choice) },
                                color = color,
                                modifier = Modifier.weight(1f).height(64.dp).semantics { contentDescription = description },
                            ) {
                                Text(choice, style = MaterialTheme.typography.headlineMedium.copy(shadow = null), color = Color(0xFF1A0D10))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.m))
            Box(Modifier.height(64.dp).fillMaxWidth()) {
                if (ui.showingAnswer && !ui.wasCorrect) {
                    PixelButton(onClick = onNext, color = SpireColors.Gold, modifier = Modifier.fillMaxSize()) {
                        Text(stringResource(R.string.practice_next).uppercase(), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.m))
        }
    }
}
