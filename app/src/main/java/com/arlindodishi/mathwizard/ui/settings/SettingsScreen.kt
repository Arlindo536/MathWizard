package com.arlindodishi.mathwizard.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arlindodishi.mathwizard.R
import com.arlindodishi.mathwizard.appContainer
import com.arlindodishi.mathwizard.data.Setting
import com.arlindodishi.mathwizard.data.Settings
import com.arlindodishi.mathwizard.ui.pixel.PixelButton
import com.arlindodishi.mathwizard.ui.pixel.PixelToggleRow
import com.arlindodishi.mathwizard.ui.theme.SpireColors
import com.arlindodishi.mathwizard.ui.theme.Spacing
import com.arlindodishi.mathwizard.ui.upgrades.ScreenHeader
import kotlinx.coroutines.launch

/** Language tags offered in Settings; "" follows the system language. */
private val languages = listOf("" to R.string.language_system, "sq" to R.string.language_sq, "en" to R.string.language_en)

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = context.appContainer.settings
    val settings by repository.settings.collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    SettingsScreen(
        settings = settings,
        language = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
        version = version,
        onToggle = { setting, value -> scope.launch { repository.set(setting, value) } },
        // Recreates the activity in the new language; stored by AppCompat (and by Android 13+).
        onLanguage = { tag -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    language: String,
    version: String,
    onToggle: (Setting, Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SpireColors.Night).safeDrawingPadding()) {
        ScreenHeader(title = stringResource(R.string.settings), onBack = onBack)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.m)) {
            Section(stringResource(R.string.settings_game))
            PixelToggleRow(stringResource(R.string.sound_toggle_cd), settings.soundOn, { onToggle(Setting.SOUND, it) }, Modifier.fillMaxWidth())
            PixelToggleRow(stringResource(R.string.music_toggle_cd), settings.musicOn, { onToggle(Setting.MUSIC, it) }, Modifier.fillMaxWidth())
            PixelToggleRow(stringResource(R.string.settings_haptics), settings.hapticsOn, { onToggle(Setting.HAPTICS, it) }, Modifier.fillMaxWidth())
            PixelToggleRow(
                stringResource(R.string.settings_relaxed),
                settings.relaxed,
                { onToggle(Setting.RELAXED, it) },
                Modifier.fillMaxWidth(),
                description = stringResource(R.string.settings_relaxed_desc),
            )

            Section(stringResource(R.string.settings_language))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { (tag, label) ->
                    val selected = language == tag || (tag.isNotEmpty() && language.startsWith(tag))
                    PixelButton(
                        onClick = { if (!selected) onLanguage(tag) },
                        color = if (selected) SpireColors.Gold else SpireColors.SurfaceRaised,
                        contentColor = if (selected) SpireColors.Night else SpireColors.Text,
                        modifier = Modifier.semantics { this.selected = selected },
                    ) {
                        Text(stringResource(label).uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Section(stringResource(R.string.settings_classic))
            PixelToggleRow(stringResource(R.string.toggle_tts), settings.ttsOn, { onToggle(Setting.TTS, it) }, Modifier.fillMaxWidth())
            PixelToggleRow(stringResource(R.string.toggle_high_contrast), settings.highContrast, { onToggle(Setting.HIGH_CONTRAST, it) }, Modifier.fillMaxWidth())

            Section(stringResource(R.string.settings_about))
            Text(stringResource(R.string.settings_credits), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            Spacer(Modifier.height(Spacing.s))
            Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodyMedium, color = SpireColors.TextMuted)
            Spacer(Modifier.height(Spacing.l))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(Spacing.l))
    Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, color = SpireColors.Gold)
    Spacer(Modifier.height(Spacing.xs))
}

