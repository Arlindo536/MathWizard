package com.arlindodishi.mathwizard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

data class Question(val text: String, val answer: Int, val options: List<Int>)

enum class BattleResult { WIN, LOSE }

data class DamageEvent(val amount: Int, val onMonster: Boolean)

class QuizViewModel : ViewModel() {

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _currentQuestion = MutableLiveData<Question>()
    val currentQuestion: LiveData<Question> = _currentQuestion

    var difficulty: String = "EASY"

    // Statistics Trackers
    var totalQuestionsAnswered = 0
    var totalCorrectAnswers = 0

    // --- Battle state ---
    var monsterMaxHealth = 100
        private set
    var playerMaxHealth = 100
        private set

    private val _monsterHealth = MutableLiveData(100)
    val monsterHealth: LiveData<Int> = _monsterHealth

    private val _playerHealth = MutableLiveData(100)
    val playerHealth: LiveData<Int> = _playerHealth

    private val _streak = MutableLiveData(0)
    val streak: LiveData<Int> = _streak

    private val _battleResult = MutableLiveData<BattleResult?>(null)
    val battleResult: LiveData<BattleResult?> = _battleResult

    private val _damageEvent = MutableLiveData<DamageEvent?>(null)
    val damageEvent: LiveData<DamageEvent?> = _damageEvent

    private var baseDamage = 18
    private var battleStarted = false

    /** Call once when entering the quiz, before the first generateNextQuestion(). */
    fun setupBattle(diff: String) {
        if (battleStarted && difficulty == diff) return
        battleStarted = true
        difficulty = diff
        resetBattleState()
    }

    /** Force a fresh battle against the same difficulty (used by "Play Again"). */
    fun restartBattle() {
        resetBattleState()
    }

    private fun resetBattleState() {
        when (difficulty) {
            "EASY" -> {
                monsterMaxHealth = 60
                baseDamage = 20
            }
            "MEDIUM" -> {
                monsterMaxHealth = 100
                baseDamage = 18
            }
            "HARD" -> {
                monsterMaxHealth = 150
                baseDamage = 16
            }
        }
        playerMaxHealth = 100
        _monsterHealth.value = monsterMaxHealth
        _playerHealth.value = playerMaxHealth
        _streak.value = 0
        _battleResult.value = null
    }

    fun generateNextQuestion() {
        var op1 = 1
        var op2 = 1
        var isAddition = true
        var text = ""
        var answer = 0

        when (difficulty) {
            "EASY" -> {
                // Addition 1-10
                op1 = Random.nextInt(1, 10)
                op2 = Random.nextInt(1, 10)
                isAddition = true
            }
            "MEDIUM" -> {
                // Add/Sub 1-20
                op1 = Random.nextInt(1, 20)
                op2 = Random.nextInt(1, 20)
                isAddition = Random.nextBoolean()
                // Avoid negatives in subtraction
                if (!isAddition && op1 < op2) {
                    val temp = op1
                    op1 = op2
                    op2 = temp
                }
            }
            "HARD" -> {
                // Multiplication 2-12
                op1 = Random.nextInt(2, 12)
                op2 = Random.nextInt(2, 12)
                isAddition = false // Using as multiplication here
            }
        }

        if (difficulty == "HARD" || (!isAddition && difficulty == "MEDIUM")) {
            if (difficulty == "HARD") {
                text = "$op1 * $op2"
                answer = op1 * op2
            } else {
                text = "$op1 - $op2"
                answer = op1 - op2
            }
        } else {
            text = "$op1 + $op2"
            answer = op1 + op2
        }

        val optionsMutable = mutableListOf<Int>()
        optionsMutable.add(answer)

        while (optionsMutable.size < 4) {
            val variance = if (difficulty == "HARD") Random.nextInt(-10, 11) else Random.nextInt(-5, 6)
            var wrong = answer + variance
            if (wrong < 0 && difficulty != "MEDIUM") wrong = 1 // Basic protection
            if (variance != 0 && !optionsMutable.contains(wrong)) {
                optionsMutable.add(wrong)
            }
        }
        optionsMutable.shuffle()

        _currentQuestion.value = Question(text, answer, optionsMutable)
    }

    /** Returns true if the answer was correct. Also drives the battle: damages the
     *  monster on a correct answer (scaled by streak combo), or the player on a
     *  wrong one, and sets [battleResult] once either health bar reaches zero. */
    fun checkAnswer(selectedOption: Int): Boolean {
        totalQuestionsAnswered++
        val correct = selectedOption == _currentQuestion.value?.answer

        if (_battleResult.value != null) {
            // Battle already decided; ignore further taps (defensive, UI should
            // already have disabled the buttons by this point).
            return correct
        }

        if (correct) {
            totalCorrectAnswers++
            _score.value = (_score.value ?: 0) + 10

            val newStreak = (_streak.value ?: 0) + 1
            _streak.value = newStreak
            val multiplier = when {
                newStreak >= 5 -> 3
                newStreak >= 3 -> 2
                else -> 1
            }
            val damage = baseDamage * multiplier
            val newMonsterHealth = ((_monsterHealth.value ?: monsterMaxHealth) - damage).coerceAtLeast(0)
            _monsterHealth.value = newMonsterHealth
            _damageEvent.value = DamageEvent(damage, onMonster = true)

            if (newMonsterHealth <= 0) {
                _battleResult.value = BattleResult.WIN
            }
        } else {
            _streak.value = 0
            val newPlayerHealth = ((_playerHealth.value ?: playerMaxHealth) - baseDamage).coerceAtLeast(0)
            _playerHealth.value = newPlayerHealth
            _damageEvent.value = DamageEvent(baseDamage, onMonster = false)

            if (newPlayerHealth <= 0) {
                _battleResult.value = BattleResult.LOSE
            }
        }

        return correct
    }
}
