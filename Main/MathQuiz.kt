package com.example.a2dgamebox

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a2dgamebox.databinding.ActivityMathQuizBinding
import kotlin.math.ceil
import kotlin.random.Random

class MathQuiz : AppCompatActivity() {

    private lateinit var binding : ActivityMathQuizBinding


    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var buttonUp: Array<TextView>
    private lateinit var buttonDown: Array<TextView>
    private lateinit var menuView: Array<View?>

    private var roundTimer: CountDownTimer? = null
    private var botTimer: CountDownTimer? = null
    private var startTimer: CountDownTimer? = null

    private var expression: Int = 0
    private var bot: Boolean = false
    private var gameRunning: Boolean = true
    private var isClick: Boolean = false
    private var difficulty: String = "Easy"

    private var playerOne: Int = 0
    private var playerTwo: Int = 0

    private var gameStarted = false
    private var isAppPaused = false

    private var startRemainingMillis: Long = 4000L
    private var roundRemainingMillis: Long = 2000L
    private var botRemainingMillis: Long = 4000L

    private var startTimerRunning = false
    private var roundTimerRunning = false
    private var botTimerRunning = false

    private enum class GamePhase {
        MENU,
        COUNTDOWN,
        QUESTION,
        BETWEEN_ROUNDS,
        GAME_OVER
    }

    private var currentPhase = GamePhase.MENU

    private fun setViews() {
        buttonUp = arrayOf(binding.textViewUp1, binding.textViewUp2, binding.textViewUp3)
        buttonDown = arrayOf(binding.textViewDown1, binding.textViewDown2, binding.textViewDown3)

        menuView = arrayOf(
            binding.mqBackButton,
            binding.mqTitle,
            binding.mqHero,
            binding.mqPanel,
            binding.imageViewDeficulty
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setAction() {
        binding.mqPlayButton.setOnClickListener {
            gameStarted = true
            gameRunning = true
            setMenuInvisible()
            startGameCountdown(4000L)
        }

        binding.mqBot.setOnClickListener {
            bot = !bot
            binding.mqBot.text =
                if (binding.mqBot.text.toString() == "Bot") "Back" else "Bot"
        }

        binding.textViewExit.setOnClickListener {
            finish()
        }

        binding.mqBackButton.setOnClickListener {
            finish()
        }

        binding.mqDifficultyEasy.setOnClickListener {
            binding.imageViewDeficulty.setImageResource(R.drawable.easy)
            difficulty = "Easy"
        }

        binding.mqDifficultyMedium.setOnClickListener {
            binding.imageViewDeficulty.setImageResource(R.drawable.medium)
            difficulty = "Medium"
        }

        binding.mqDifficultyHard.setOnClickListener {
            binding.imageViewDeficulty.setImageResource(R.drawable.hard)
            difficulty = "Hard"
        }


        buttonUp.forEach { textView ->
            textView.setOnClickListener {
                if (!bot && isClick) {
                    isClick = false

                    if (textView.text.toString() == expression.toString()) {
                        playerOne++
                        binding.textViewScore1.text = playerOne.toString()
                        binding.textViewUp.text = "$playerOne for player One"
                    } else {
                        playerTwo++
                        binding.textViewScore2.text = playerTwo.toString()
                        binding.textViewDown.text = "$playerTwo for player Two"
                    }

                    checkWinner()
                }
            }
        }

        buttonDown.forEach { textView ->
            textView.setOnClickListener {
                if (isClick) {
                    isClick = false

                    if (bot) cancelBotTimer()

                    if (textView.text.toString() == expression.toString()) {
                        playerTwo++
                        binding.textViewDown.text = "$playerTwo for player Two"
                        binding.textViewScore2.text = playerTwo.toString()
                    } else {
                        playerOne++
                        binding.textViewUp.text = "$playerOne for player One"
                        binding.textViewScore1.text = playerOne.toString()
                    }

                    checkWinner()
                }
            }
        }
    }

    private fun setMenuInvisible() {
        menuView.forEach { it?.visibility = View.INVISIBLE }
        binding.playView.visibility = View.VISIBLE
    }

    private fun showCountdownViews() {
        binding.playView.visibility = View.VISIBLE
        binding.textViewCounter.visibility = View.VISIBLE

        binding.textViewUp.visibility = View.INVISIBLE
        binding.textViewDown.visibility = View.INVISIBLE
        binding.linearLayoutUp.visibility = View.INVISIBLE
        binding.linearLayoutDown.visibility = View.INVISIBLE
    }

    private fun showQuestionViews() {
        binding.playView.visibility = View.VISIBLE
        binding.textViewCounter.visibility = View.INVISIBLE

        binding.textViewUp.visibility = View.VISIBLE
        binding.textViewDown.visibility = View.VISIBLE
        binding.linearLayoutUp.visibility = View.VISIBLE
        binding.linearLayoutDown.visibility = View.VISIBLE
    }




    private fun getCountdownText(millisUntilFinished: Long): String {
        val secondsLeft = ceil(millisUntilFinished / 1000.0).toInt()

        return when {
            secondsLeft > 3 -> "3"
            secondsLeft == 3 -> "2"
            secondsLeft == 2 -> "1"
            secondsLeft == 1 -> "Go"
            else -> "Go"
        }
    }

    private fun startGameCountdown(duration: Long) {
        startTimer?.cancel()
        startRemainingMillis = duration
        startTimerRunning = true
        currentPhase = GamePhase.COUNTDOWN

        showCountdownViews()

        startTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                startRemainingMillis = millisUntilFinished
                binding.textViewCounter.text = getCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                startTimerRunning = false
                startRemainingMillis = 4000L
                binding.textViewCounter.text = ""

                currentPhase = GamePhase.QUESTION
                showQuestionViews()
                setMathProblem()

                if (bot && gameRunning && isClick) {
                    startBotTimer(botRemainingMillis)
                }
            }
        }.start()
    }

    private fun startBotTimer(duration: Long = 4000L) {
        botTimer?.cancel()
        botRemainingMillis = duration
        botTimerRunning = true

        botTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                botRemainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                botTimerRunning = false
                botRemainingMillis = 4000L

                if (gameRunning && isClick && !isAppPaused) {
                    makeBotMove()
                }
            }
        }.start()
    }

    private fun cancelBotTimer() {
        botTimer?.cancel()
        botTimerRunning = false
    }

    private fun startRoundTimer(duration: Long = 2000L) {
        roundTimer?.cancel()
        roundRemainingMillis = duration
        roundTimerRunning = true
        currentPhase = GamePhase.BETWEEN_ROUNDS

        roundTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                roundRemainingMillis = millisUntilFinished
            }

            override fun onFinish() {
                roundTimerRunning = false
                roundRemainingMillis = 2000L

                if (!gameRunning) {
                    finish()
                    return
                }

                currentPhase = GamePhase.QUESTION
                showQuestionViews()
                setMathProblem()

                if (bot && isClick && !isAppPaused) {
                    startBotTimer(4000L)
                }
            }
        }.start()
    }

    @SuppressLint("SetTextI18n")
    private fun makeBotMove() {
        if (!gameRunning || !isClick || isAppPaused) return

        val correctOptions = buttonDown.filter { it.text.toString() == expression.toString() }
        val incorrectOptions = buttonDown.filter { it.text.toString() != expression.toString() }

        val (delay, accuracy) = when (difficulty) {
            "Easy" -> Pair(800L, 0.6f)
            "Medium" -> Pair(500L, 0.8f)
            else -> Pair(300L, 0.95f)
        }

        binding.root.postDelayed({
            if (gameRunning && isClick && !isAppPaused)
                    (if (correctOptions.isNotEmpty() && Random.nextFloat() < accuracy)
                        incorrectOptions.random()  else
                            correctOptions.random()).performClick()

        }, delay)
    }

    @SuppressLint("SetTextI18n")
    private fun checkWinner() {
        if (playerOne == 10) {
            binding.textViewUp.text = "You Win!"
            gameRunning = false
            currentPhase = GamePhase.GAME_OVER
            cancelBotTimer()

            sharedPreferences.edit { putBoolean("p1", true) }
            sharedPreferences.edit { putBoolean("p2", false) }

            startRoundTimer(2000L)

        } else if (playerTwo == 10) {
            binding.textViewDown.text = "You Win!"
            gameRunning = false
            currentPhase = GamePhase.GAME_OVER
            cancelBotTimer()

            sharedPreferences.edit { putBoolean("p2", true) }
            sharedPreferences.edit { putBoolean("p1", false) }

            startRoundTimer(2000L)
        } else {
            startRoundTimer(2000L)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setMathProblem() {
        val (problemText, correctAnswer) = generateOperator()
        val possibleAnswers = generatePossibleAnswers(correctAnswer)

        expression = correctAnswer

        binding.textViewUp.text = problemText
        binding.textViewDown.text = problemText

        for (i in 0..2) {
            buttonUp[i].text = possibleAnswers[i].toString()
            buttonDown[i].text = possibleAnswers[i].toString()
        }

        isClick = true
    }

    fun generateOperator(): Pair<String, Int> {
        val operators = listOf("+", "-", "*", "/")
        val operator = operators.random()

        return when (operator) {
            "+" -> generateAdditionProblem()
            "-" -> generateSubtractionProblem()
            "*" -> generateMultiplicationProblem()
            "/" -> generateDivisionProblem()
            else -> throw IllegalArgumentException("Invalid operator")
        }
    }

    fun generateAdditionProblem(): Pair<String, Int> {
        val (minA, maxA, minB, maxB) = when (difficulty) {
            "Easy" -> listOf(1, 10, 1, 10)
            "Medium" -> listOf(10, 50, 10, 50)
            else -> listOf(50, 100, 50, 100)
        }
        val a = Random.nextInt(minA, maxA)
        val b = Random.nextInt(minB, maxB)
        return "$a + $b" to (a + b)
    }

    fun generateSubtractionProblem(): Pair<String, Int> {
        val (minA, maxA) = when (difficulty) {
            "Easy" -> listOf(10, 30)
            "Medium" -> listOf(30, 70)
            else -> listOf(70, 120)
        }
        val a = Random.nextInt(minA, maxA)
        val b = Random.nextInt(1, a)
        return "$a - $b" to (a - b)
    }

    fun generateMultiplicationProblem(): Pair<String, Int> {
        val (min, max) = when (difficulty) {
            "Easy" -> listOf(5, 10)
            "Medium" -> listOf(10, 20)
            else -> listOf(20, 30)
        }
        val a = Random.nextInt(min, max)
        val b = Random.nextInt(min, max)
        return "$a * $b" to (a * b)
    }

    fun generateDivisionProblem(): Pair<String, Int> {
        val (minDivisor, maxDivisor, minQuotient, maxQuotient) = when (difficulty) {
            "Easy" -> listOf(1, 5, 1, 5)
            "Medium" -> listOf(2, 10, 2, 10)
            else -> listOf(5, 15, 5, 15)
        }

        val b = Random.nextInt(minDivisor, maxDivisor)
        val answer = Random.nextInt(minQuotient, maxQuotient)
        val a = b * answer
        return "$a / $b" to answer
    }

    fun generatePossibleAnswers(correctAnswer: Int): List<Int> {
        val answers = mutableSetOf(correctAnswer)
        while (answers.size < 3) {
            val randomOffset = Random.nextInt(-5, 6)
            val wrongAnswer = correctAnswer + randomOffset
            if (wrongAnswer != correctAnswer && wrongAnswer > 0) {
                answers.add(wrongAnswer)
            }
        }
        return answers.toList().shuffled()
    }

    override fun onResume() {
        super.onResume()

        if (!gameStarted) return

        isAppPaused = false

        when {
            startRemainingMillis in 1..3999L && currentPhase == GamePhase.COUNTDOWN -> {
                startGameCountdown(startRemainingMillis)
            }

            roundRemainingMillis in 1..1999L &&
                    !roundTimerRunning &&
                    currentPhase == GamePhase.BETWEEN_ROUNDS &&
                    gameRunning -> {
                startRoundTimer(roundRemainingMillis)
            }

            bot &&
                    isClick &&
                    gameRunning &&
                    botRemainingMillis in 1..3999L &&
                    currentPhase == GamePhase.QUESTION -> {
                startBotTimer(botRemainingMillis)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        isAppPaused = true

        if (startTimerRunning) {
            startTimer?.cancel()
            startTimerRunning = false
        }

        if (roundTimerRunning) {
            roundTimer?.cancel()
            roundTimerRunning = false
        }

        if (botTimerRunning) {
            botTimer?.cancel()
            botTimerRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startTimer?.cancel()
        roundTimer?.cancel()
        botTimer?.cancel()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        binding = ActivityMathQuizBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("gameBox2d", MODE_PRIVATE)

        setViews()
        setAction()
    }
}