# MathWizard: design decisions

## 1. Audit of the existing app (v1.0, commit 4b69140)

**Stack:** Kotlin, XML Views + `findViewById` (viewBinding enabled but unused), AGP 8.13.2,
Kotlin 1.9.22, Gradle 8.13, compileSdk/targetSdk 34, minSdk 24, Java 8 target.

**Structure:** five Activities, one ViewModel.

| File | Role |
|---|---|
| `MainActivity` | Menu: Play / Settings / High score |
| `DifficultyActivity` | Easy / Medium / Hard, passed as a String intent extra |
| `QuizActivity` (311 lines) | Battle screen: question, 4 answer buttons, monster/player HP bars, combo, damage popup, result overlay, TTS, haptics, high-contrast tweaks, saves stats in `onDestroy` |
| `QuizViewModel` (195 lines) | Question generation + battle rules, exposed as `LiveData` |
| `HighScoreActivity` | Best score, questions answered, accuracy |
| `SettingsActivity` | TTS, haptics, high contrast, reset stats |

**Question generation:** inside `QuizViewModel.generateNextQuestion()`, using unseeded
`kotlin.random.Random`. Easy = addition 1-9, Medium = add/sub 1-19 (no negatives),
Hard = multiplication 2-11 (`nextInt` upper bound is exclusive, so 12 never appears).
Wrong options are `answer ± variance`. Not reproducible, not testable, mixed with Android code.

**Battle:** monster HP 60/100/150 by difficulty, player HP 100; correct = base damage x
streak multiplier (x2 at 3, x3 at 5), wrong = player takes base damage.

**Persistence:** `SharedPreferences` only (`Settings`: tts/haptics/contrast; `Scores`:
best_score, total_played, total_correct). Stats are written in `onDestroy`, which is not
guaranteed to run.

**Quality:** builds; lint 0 errors / 20 warnings; **no unit or UI tests**; no CI.
Strings are English only. Hardcoded "Statistics Reset" toast.

**Known small issues:** high-contrast colours applied piecemeal per screen; TTS always
`Locale.US`; difficulty as raw Strings; `Question` options are rendered by parsing button
text back to Int.

## 2. Decision: Views vs Compose (proposal, pending owner approval)

**Proposal: rebuild the UI in Jetpack Compose (single Activity, Navigation Compose),
and move game rules into a pure Kotlin `:engine` module.**

Reasons:
- The roguelite needs Canvas drawing, particles, parallax, screen shake and many animated
  screens; Compose makes these far cheaper than custom Views.
- The existing UI is small (5 screens, ~500 lines of XML), so migration cost is low.
- Testability: a pure Kotlin engine can be unit-tested on the JVM with fixed seeds.

What we keep: the battle idea (damage, streak combo), haptics patterns, TTS and
high-contrast concepts (they become settings in the new app), app id and signing setup.

What changes in the toolchain (done in step 2/3, not now):
- Kotlin 1.9.22 -> 2.x (required for the Compose compiler plugin), Java target 8 -> 17.
- Add Compose BOM, Navigation, Lifecycle/ViewModel Compose, later Room + DataStore.
- Old Activities stay runnable until their Compose replacements exist, then are removed
  (with owner approval).

## 3. Build environment notes (Fedora)

- Gradle 8.13 does not run on JDK 25 (system default and Android Studio's bundled JBR).
  Use JDK 17 or 21: `JAVA_HOME=~/.jdks/jdk-21.0.13+9 ./gradlew assembleDebug`.
- Android SDK at `~/Android/Sdk` (platforms 34 and 36.1 installed).
