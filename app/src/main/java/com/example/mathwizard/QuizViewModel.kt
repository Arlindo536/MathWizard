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

    init {
        generateNextQuestion()
    }

    fun generateNextQuestion() {
        val op1 = Random.nextInt(1, 10)
        val op2 = Random.nextInt(1, 10)
        
        val isAddition = Random.nextBoolean()
        val text = if (isAddition) "$op1 + $op2" else "$op1 * $op2"
        val answer = if (isAddition) op1 + op2 else op1 * op2

        val optionsMutable = mutableListOf<Int>()
        optionsMutable.add(answer)
        while(optionsMutable.size < 4) {
            val wrong = answer + Random.nextInt(-5, 6)
            if (wrong > 0 && !optionsMutable.contains(wrong)) {
                optionsMutable.add(wrong)
            }
        }
        optionsMutable.shuffle()
        
        _currentQuestion.value = Question(text, answer, optionsMutable)
    }

    fun checkAnswer(selectedOption: Int): Boolean {
        val correct = selectedOption == _currentQuestion.value?.answer
        if (correct) {
            _score.value = (_score.value ?: 0) + 10
        }
        return correct
    }
}
