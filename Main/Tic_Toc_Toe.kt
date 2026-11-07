package com.example.a2dgamebox

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.a2dgamebox.databinding.ActivityTicTacToeMenuBinding
import kotlin.random.Random

class Tic_Toc_Toe : AppCompatActivity() {

    private lateinit var binding: ActivityTicTacToeMenuBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var game: TicTacToeGame

    private var botState: Boolean = false

    // false = X turn, true = O turn
    private var userState: Boolean = false

    private var countDownTimer: CountDownTimer? = null

    private lateinit var viewsMenu: Array<View?>
    private lateinit var image: Array<Array<ImageView?>>
    private lateinit var victor: Array<View?>
    private lateinit var mainView: Array<View?>

    private lateinit var btnDifficulty : Array<RadioButton>

    private var textMode = "";

    private val baseColor = arrayOf(
        Color.rgb(131, 54, 79),   // X = Red
        Color.rgb(30, 102, 157)   // O = Blue
    )

    private val crossCircle = arrayOf(
        R.drawable.cross,   // X
        R.drawable.circle   // O
    )

    private fun setViews() {
        mainView = arrayOf(
            binding.linearLayout,
            binding.linearLayout2,
            binding.linearLayout3,
            binding.divider13,
            binding.divider14
        )

        image = arrayOf(
            arrayOf(binding.image1, binding.image2, binding.image3),
            arrayOf(binding.image4, binding.image5, binding.image6),
            arrayOf(binding.image7, binding.image8, binding.image9)
        )

        victor = arrayOf(
            binding.dividerWin1,
            binding.dividerWin2,
            binding.dividerWin3,
            binding.dividerWin4,
            binding.dividerWin5,
            binding.dividerWin6,
            binding.dividerWin7,
            binding.dividerWin8
        )

        // Hide menu elements only. The root also contains the game board.
        viewsMenu = arrayOf(
            binding.tttPanel,
            binding.tttTitle,
            binding.tttBackButton,
            binding.tttHero,
            binding.imageViewMode
        )

        btnDifficulty = arrayOf(binding.tttDifficultyEasy,
            binding.tttDifficultyMedium,
            binding.tttDifficultyHard)

        initializeBotTimer()
    }

    @SuppressLint("SetTextI18n")
    private fun setActions() {
        binding.tttPlayButton.setOnClickListener {
            viewsMenu.forEach { it?.visibility = View.INVISIBLE }
            binding.exit.visibility = View.VISIBLE
            startGame()
        }

        binding.tttBackButton.setOnClickListener {
            finish()
        }

        binding.tttDifficultyEasy.setOnClickListener {
            binding.imageViewMode.setImageResource(R.drawable.easy)
            textMode = "easy"
        }

        binding.tttDifficultyMedium.setOnClickListener {
            binding.imageViewMode.setImageResource(R.drawable.medium)
            textMode = "medium"
        }

        binding.tttDifficultyHard.setOnClickListener {
            binding.imageViewMode.setImageResource(R.drawable.hard)
            textMode = "hard"
        }


        binding.exit.setOnClickListener {
            finish()
        }

        binding.tttBot.setOnClickListener {
            botState = !botState
             if (botState){
                 binding.tttBot.text = "Back"
                 btnDifficulty.forEach {
                     it.visibility = View.VISIBLE
                 }
            } else{
                 binding.tttBot.text = "Bot"
                btnDifficulty.forEach {
                    it.visibility = View.INVISIBLE
                }
            }
        }


        binding.textViewRestart.setOnClickListener()
        {
            restartGame()
        }

        for (row in 0..2)
        {
            for (col in 0..2) {
                image[row][col]?.setOnClickListener {
                    onCellClickedUsers(row, col)
                }
            }
        }
    }

    private fun initializeBotTimer() {
        countDownTimer = object : CountDownTimer(700, 700) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                if (!botState) return
                if (game.checkWinner() != Player.NONE) return
                if (isBoardFull()) return

                val move = game.aiMove(textMode)

                move?.let { (row, col) ->
                    if (game.board[row][col] == Player.NONE) {
                        // bot = O
                        game.board[row][col] = Player.O
                        image[row][col]?.setImageResource(crossCircle[1])

                        userState = false // next is human X
                        val winner = checkGameState()

                        if (winner == Player.NONE && !isBoardFull()) {
                            isEnable(true)
                            binding.activityTicTocToeMain.setBackgroundColor(baseColor[0])
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTicTacToeMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.activityTicTocToeMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("gameBox2d", MODE_PRIVATE)

        game = TicTacToeGame()
        setViews()
        setActions()
    }

    private fun onCellClickedUsers(row: Int, col: Int) {
        if (game.board[row][col] != Player.NONE) return
        if (game.checkWinner() != Player.NONE) return

        // In bot mode, only human X can tap
        if (botState && userState) return

        if (userState) {
            // O turn
            game.board[row][col] = Player.O
            image[row][col]?.setImageResource(crossCircle[1])
        } else {
            // X turn
            game.board[row][col] = Player.X
            image[row][col]?.setImageResource(crossCircle[0])
        }

        userState = !userState
        val winner = checkGameState()

        if (winner != Player.NONE || isBoardFull()) {
            isEnable(false)
            return
        }

        if (botState) {
            // after human X move, bot O plays
            isEnable(false)
            binding.activityTicTocToeMain.setBackgroundColor(baseColor[1])
            countDownTimer?.start()
        } else {
            binding.activityTicTocToeMain.setBackgroundColor(if (userState) baseColor[1] else baseColor[0])
            isEnable(true)
        }
    }

    private fun isEnable(state: Boolean) {
        for (row in 0..2) {
            for (col in 0..2) {
                image[row][col]?.isEnabled = state
            }
        }
    }

    private fun isBoardFull(): Boolean {
        return game.board.flatten().none { it == Player.NONE }
    }

    private fun checkGameState(): Player {
        val winner = game.checkWinner()

        when (winner) {
            Player.X, Player.O -> {
                val winPattern = getWinPatternFromBoard(game.board)
                if (winPattern > 0) {
                    showWinLine(winPattern)
                }

                Toast.makeText(this, "Player ${winner.name} wins!", Toast.LENGTH_SHORT).show()
                binding.textViewRestart.visibility = View.VISIBLE
                countDownTimer?.cancel()
                isEnable(false)

                if (winner == Player.X) {
                    sharedPreferences.edit {
                        putBoolean("p2", true)
                        putBoolean("p1", false)
                    }
                } else {
                    sharedPreferences.edit {
                        putBoolean("p1", true)
                        putBoolean("p2", false)
                    }
                }
            }

            Player.NONE -> {
                if (isBoardFull()) {
                    Toast.makeText(this, "Draw", Toast.LENGTH_SHORT).show()
                    binding.textViewRestart.visibility = View.VISIBLE
                    countDownTimer?.cancel()
                    isEnable(false)
                }
            }
        }

        return winner
    }

    private fun showWinLine(winPattern: Int) {
        victor.forEach { it?.visibility = View.INVISIBLE }

        when (winPattern) {
            1 -> binding.dividerWin4.visibility = View.VISIBLE
            2 -> binding.dividerWin5.visibility = View.VISIBLE
            3 -> binding.dividerWin6.visibility = View.VISIBLE
            4 -> binding.dividerWin1.visibility = View.VISIBLE
            5 -> binding.dividerWin2.visibility = View.VISIBLE
            6 -> binding.dividerWin3.visibility = View.VISIBLE
            7 -> binding.dividerWin8.visibility = View.VISIBLE
            8 -> binding.dividerWin7.visibility = View.VISIBLE
        }
    }

    private fun getWinPatternFromBoard(board: Array<Array<Player>>): Int {
        return when {
            board[0][0] != Player.NONE && board[0][0] == board[0][1] && board[0][1] == board[0][2] -> 1
            board[1][0] != Player.NONE && board[1][0] == board[1][1] && board[1][1] == board[1][2] -> 2
            board[2][0] != Player.NONE && board[2][0] == board[2][1] && board[2][1] == board[2][2] -> 3
            board[0][0] != Player.NONE && board[0][0] == board[1][0] && board[1][0] == board[2][0] -> 4
            board[0][1] != Player.NONE && board[0][1] == board[1][1] && board[1][1] == board[2][1] -> 5
            board[0][2] != Player.NONE && board[0][2] == board[1][2] && board[1][2] == board[2][2] -> 6
            board[0][0] != Player.NONE && board[0][0] == board[1][1] && board[1][1] == board[2][2] -> 7
            board[0][2] != Player.NONE && board[0][2] == board[1][1] && board[1][1] == board[2][0] -> 8
            else -> 0
        }
    }

    private fun startGame() {
        game.resetBoard()

        for (row in 0..2) {
            for (col in 0..2) {
                image[row][col]?.setImageResource(0)
            }
        }

        victor.forEach { it?.visibility = View.INVISIBLE }
        binding.textViewRestart.visibility = View.INVISIBLE
        countDownTimer?.cancel()
        mainView.forEach { it?.visibility = View.VISIBLE }

        if (botState) {
            // human is X, bot is O
            userState = false
            binding.activityTicTocToeMain.setBackgroundColor(baseColor[0])
            isEnable(true)
        } else {
            // two-player random starter
            userState = Random.nextBoolean()
            binding.activityTicTocToeMain.setBackgroundColor(if (userState) baseColor[1] else baseColor[0])
            isEnable(true)
        }
    }

    private fun restartGame() {
        game.resetBoard()

        for (row in 0..2) {
            for (col in 0..2) {
                image[row][col]?.setImageResource(0)
            }
        }

        victor.forEach { it?.visibility = View.INVISIBLE }
        binding.textViewRestart.visibility = View.INVISIBLE
        countDownTimer?.cancel()

        if (botState) {
            userState = false
            binding.activityTicTocToeMain.setBackgroundColor(baseColor[0])
            isEnable(true)
        } else {
            userState = Random.nextBoolean()
            binding.activityTicTocToeMain.setBackgroundColor(if (userState) baseColor[1] else baseColor[0])
            isEnable(true)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}