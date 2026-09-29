# Math Wizard

A simple, accessible math quiz for Android — now framed as a duel: answer
correctly to damage the monster, answer wrong and it hits back.

## Features

- Three difficulty levels: Easy (addition), Medium (addition/subtraction),
  Hard (multiplication)
- Battle mode: monster and player health bars, streak combo multiplier,
  floating damage numbers, win/lose screen
- Accessibility: text-to-speech reads each question aloud, adjustable
  haptic feedback, high-contrast display mode
- Local high scores and accuracy tracking (best score, total played,
  accuracy %), stored on-device

## Tech

Kotlin, single-Activity-per-screen with `ViewModel` + `LiveData`
(`QuizViewModel` holds the battle state), View Binding, Material
Components, `ConstraintLayout`. No backend — everything is local
(`SharedPreferences`).

## Building a release APK

1. Generate a signing key once (keep the resulting `.jks` file and its
   passwords safe — losing them means you can never update this exact
   app again):

   ```bash
   keytool -genkeypair -v -keystore mathwizard-release.jks \
     -alias mathwizard -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy `keystore.properties.example` to `keystore.properties` in the
   project root and fill in the real values (this file is git-ignored,
   never commit it).

3. In Android Studio: **Build → Generate Signed Bundle / APK**, or from
   the command line:

   ```bash
   ./gradlew assembleRelease
   ```

   The signed APK lands in `app/build/outputs/apk/release/`.

## Project structure

```
app/src/main/java/com/arlindodishi/mathwizard/
  MainActivity.kt        – home screen
  DifficultyActivity.kt  – difficulty picker
  QuizActivity.kt         – battle screen (UI + animations)
  QuizViewModel.kt        – question generation + battle state
  SettingsActivity.kt     – TTS / haptics / high-contrast toggles
  HighScoreActivity.kt    – local stats screen
```
