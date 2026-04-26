package com.example.mathwizard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

data class Question(val text: String, val answer: Int, val options: List<Int>)

class QuizViewModel : ViewModel() {

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _currentQuestion = MutableLiveData<Question>()
    val currentQuestion: LiveData<Question> = _currentQuestion

    var difficulty: String = "EASY"
    
    // Statistics Trackers
    var totalQuestionsAnswered = 0
    var totalCorrectAnswers = 0

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
        
        while(optionsMutable.size < 4) {
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

    fun checkAnswer(selectedOption: Int): Boolean {
        totalQuestionsAnswered++
        val correct = selectedOption == _currentQuestion.value?.answer
        if (correct) {
            totalCorrectAnswers++
            _score.value = (_score.value ?: 0) + 10
        }
        return correct
    }
}
