# Technical Report - Math Wizard

## 1. Cover Page
- **App Name**: Math Wizard
- **Team**: Arlindo Dishi
- **Date**: April 2026

## 2. Introduction
**Problem Statement**: Young students often struggle to engage with simple arithmetic problems. Traditional flashcards can be boring.
**Solution**: Math Wizard is an interactive, gamified Android application that quizzes students on basic math addition and multiplication. It utilizes audio (Text-to-Speech) to make learning accessible and engaging for children who might still be developing their reading skills.

## 3. Architecture Overview
The application uses the **MVVM (Model-View-ViewModel)** architectural pattern.
- `MainActivity` serves as the routing hub.
- `QuizActivity` relies on `QuizViewModel` for all business logic (generating random questions, validating answers, and keeping the score). 
- `SettingsActivity` and `HighScoreActivity` interact with SharedPreferences to load and store data.

*(Imagine an Architecture Diagram here connecting MainActivity via Explicit Intents to QuizActivity, SettingsActivity, and HighScoreActivity)*

## 4. Feature Description
1. **Multi-screen App & Intents**: The UI contains multiple screens navigated via Explicit Intents from the `MainActivity`.
2. **UI & Layouts**: Extensive use of `ConstraintLayout` ensures responsiveness. Custom colors and Material design components (like `MaterialButton` with corner radius) are used.
3. **Data Persistence**: `SharedPreferences` is used to persist the `best_score` and the `tts_enabled` setting across sessions.
4. **Hardware Feature - Text-To-Speech (TTS)**: The SDK's `TextToSpeech` engine is integrated in `QuizActivity` to audibly read math equations out loud ("What is 5 plus 3?"). This appeals to young users.

## 5. UI/UX Design Decisions
- **Color Palette**: Friendly Purple (`#FF6200EE`) and Teal (`#FF03DAC5`) give a vibrant, playful feel.
- **Button Sizing**: Large buttons (80dp height) for answers are easy for children to tap.
- **Feedback Colors**: Immediate correct/wrong feedback colored Green (`#FF4CAF50`) and Red (`#FFF44336`) helps reinforce learning.

## 6. Testing
| # | Test Case Description | Device / API Level | Status |
|---|------------------------|--------------------|--------|
| 1 | Launch App & Navigate to Quiz | Emulator API 34 | PASS |
| 2 | Select correct answer increases score | Emulator API 34 | PASS |
| 3 | Close App and check High Score persists | Emulator API 34 | PASS |
| 4 | Toggle TTS off in Settings mutes TTS | Emulator API 34 | PASS |

## 7. Reflection
- **What went well:** The separation of concerns using ViewModel made `QuizActivity` clean and focused purely on UI updates and TTS lifecycle management.
- **What could be improved:** Future iterations could add structured data using `SQLite` or `Room` to track the user's precision over time, or which specific equations they struggle with the most.

## 8. References
- Android Developer Documentation (Intents, TextToSpeech, SharedPreferences).
- Material Design guidelines.
