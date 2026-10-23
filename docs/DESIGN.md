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

## 2. Decision: Views vs Compose (approved 2026-10-22)

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

## 4. Engine (step 2)

Pure Kotlin module `:engine`, no Android dependencies, tested with `./gradlew :engine:test`.

- `Problems.generate(school, difficulty 0..100, seed)` -> `Problem(prompt, answer, choices)`.
  Same inputs always give the same problem (`kotlin.random.Random(seed)`).
- `Seeds`: SplitMix64-based derivation. `Seeds.problem(runSeed, floor, index)` for runs,
  `Seeds.daily(epochDay)` for the Daily Rift (epoch day instead of `java.time`, which needs
  API 26+).
- `ArithmeticGenerator`: + from 0, − from 15, × from 35, ÷ from 55, two-step expressions
  (precedence, brackets) from 75. Operand ranges grow with difficulty; no negatives, no zero
  operands, exact division. Wrong choices model real mistakes: off-by-one, carry/borrow (±10),
  neighbouring table facts, wrong evaluation order.
- `DifficultyCurve.base(floor)` = 100 · (1 − e^(−(floor−1)/30)): 0 on floor 1, ~26 on 10,
  ~55 on 25, ~80 on 50, approaching 100 (endless). `minimum(floor)` = 75% of base.
- `AdaptiveDifficulty`: rolling window of the last 8 answers per school. Fast correct
  (≤ half the target time) +3, correct +1, miss −4, two misses in a row an extra −4;
  offset clamped to −20..+15, result never below the floor minimum.

## 5. Run loop (step 3)

- Engine: `RunEngine(RunConfig)` owns one climb; `answer(choice, elapsedMillis)`, `timeOut()`,
  `nextFloor()`. Phases: FIGHTING -> FLOOR_CLEARED -> FIGHTING ... -> GAME_OVER.
  All numbers live in `Balance`: spell 25 dmg, ×1.5/×1.25 for answering in the first/second
  third of the timer, combo ×1.25 at 3 and ×1.5 at 5 in a row (carries across floors),
  enemy HP = 25 × (5..7 hits) × enemy factor, enemy hit = (10 + floor) × factor (max 35),
  +15 HP per cleared floor, timer 8-20 s (relaxed ×1.75). A floor takes 3-6 answers at
  moderate speed (tested over 50 seeds × 30 floors).
- App: single `MainActivity` (Compose) with Navigation: Home -> Run (floor-cleared overlay,
  summary on game over). `RunViewModel` drives the timer (50 ms tick, pauses in background),
  700 ms answer feedback, and records best floor in `SharedPreferences("Spire")` until Room/
  DataStore arrive in step 5. Old Classic Duel, Statistics and Settings screens are still
  the XML Activities, opened from Home.
- Strings for the new screens exist in English and Albanian (`values-sq`).

## 6. Relics and bosses (step 4)

- `Relic` is plain data (multipliers, bonuses, flags); `Relics.all` holds 12, `Modifiers.of(relics)`
  folds them. After every cleared floor the player picks 1 of 3 unheld relics (seeded per floor);
  `RunEngine.nextFloor(relicId)` applies it.
- Every 5th floor is a boss (`Bestiary.bossFor(school)`): HP ×1.5, hits ×1.25, timer ×1.5, and
  `ProblemGenerator.generateBoss` gives multi-step problems (three-term sums below 35, a × b ± c
  up to 75, then a × b + c × d or two-step forms). A boss takes 4-9 answers at moderate speed.
- Test builds are release builds signed with the owner's key (same certificate as the v1.0 APK,
  so they install over it). `versionCode` follows the step number for now.

## 7. Meta progression (step 5)

- Engine `Meta.kt`: `MetaProfile(crystals, levels)` with pure `buy()` rules (cost per level,
  prerequisite level, max level) and `bonuses()` -> `RunBonuses` passed in `RunConfig`.
  Seven upgrades in three branches: Body (Vitality +10 HP/level, Second Wind +5 heal/level),
  Mind (Focus +8% time/level, Hints: hide two wrong answers once per hint), Arcana (Arcane
  Power +5% damage/level, Relic Insight: 4 relic choices, Heirloom: start with a random relic).
  Full tree costs 1,465 crystals.
- Crystals per run = 3 × floors cleared + 10 × bosses + correct answers / 5.
- Persistence: Room database `mathwizard.db` (tables `runs`, `upgrades`, `wallet`; schema
  exported to `app/schemas`). Recording a run and buying an upgrade are single transactions.
  The step-3 best floor in `SharedPreferences("Spire")` is imported once as a legacy run.
  DataStore arrives with the settings screen (step 8).
- `AppContainer` (in `MathWizardApplication`) is the manual DI root. Repository tests run on
  the JVM with Robolectric and an in-memory database.

## 8. Spell schools (step 6)

- Five new pure generators, each with tiers by difficulty and a multi-step boss variant:
  `FractionsGenerator` (exact `Fraction` type, answers in lowest terms, percentages),
  `AlgebraGenerator` (linear equations with a positive whole solution), `PowersGenerator`
  (superscripts, exact √ and ∛), `GeometryGenerator` (word problems via `promptKey` +
  `promptArgs`, localised in the app), `SequencesGenerator` (wrong choices are rejected if they
  fit any simple rule, so each sequence has one sensible answer).
- Tests check every school independently: a rational evaluator for fractions, substitution and
  uniqueness for equations, an extended expression evaluator for powers, formula recomputation
  for geometry, and an independent rule checker for sequences.
- Each school has 3 enemies and a boss. Schools unlock in order through the Upgrades screen
  (40, 80, 120, 160, 200 crystals). The player picks a school before each climb.
- Room schema v2 adds `runs.school` (migration tested with `MigrationTestHelper`); best floor
  and "new record" are tracked per school.
- Known limitation: generators pick uniformly among unlocked kinds, so easy kinds still appear at
  high difficulty. Weighting newer kinds is part of the balancing pass (step 9).

## 9. Visual overhaul: pixel art (step 7)

- Style chosen by the owner from two mock screens: pixel art over vector-glow.
- Sprites are character grids plus palettes (`ui/pixel/Sprites.kt`): 10 body templates
  recoloured into 28 enemies (bosses 24×24), a wizard and 8×8 icons for relics, schools,
  upgrades and UI. `PixelImage` draws them with whole-pixel squares so edges stay crisp.
  `tools/preview_sprites.py` renders all of them to one PNG for review.
- Font: Jersey 10 (OFL). Pixelify Sans was tried first but its B reads as G, 5 as S at some
  sizes and its fi/fl ligatures look like A. Jersey has no superscript digits, so `problemText`
  draws exponents as raised small digits.
- `SpireBackdrop`: banded night sky, twinkling stars, moon, two tower layers and brick pillars
  with torches (school colour) that scroll with the floor, so a new floor looks like climbing.
- Juice: spell bolt and burst particles (`SpellEffect`), hit flash and shake, screen shake and
  red flash when hurt, two-step idle bob, floor/boss banner, pressable pixel buttons.
- Audio: 10 chiptune effects and a 17 s music loop generated by `tools/gen_sounds.py`
  (SoundPool / MediaPlayer in `GameFeedback`); sound and music toggles on Home; the classic
  haptics setting is respected.
- Screenshot tests (Robolectric native graphics + Roborazzi) render 9 screens to
  `docs/screenshots`; they caught a bug where spell particles never played.
