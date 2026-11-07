package com.example.a2dgamebox

import kotlin.random.Random


enum class Player { NONE, X, O }

class TicTacToeGame {
    var board = Array(3) { Array(3) { Player.NONE } }

    fun resetBoard() {
        board = Array(3) { Array(3) { Player.NONE } }
    }

    fun checkWinner(): Player {

        for (i in 0..2) {
            if (board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                if (board[i][0] != Player.NONE) return board[i][0]
            }
        }

        for (j in 0..2) {
            if (board[0][j] == board[1][j] && board[1][j] == board[2][j]) {
                if (board[0][j] != Player.NONE) return board[0][j]
            }
        }

        if (board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            if (board[0][0] != Player.NONE) return board[0][0]
        }
        if (board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            if (board[0][2] != Player.NONE) return board[0][2]
        }


        if (board.all { row -> row.all { it != Player.NONE } }) {
            return Player.NONE
        }

        return Player.NONE
    }


    fun aiMove(difficulty: String): Pair<Int, Int>? {
        val bestMoveChance = when (difficulty.trim().lowercase()) {
            "easy" -> getRandomMove()
            "medium" -> if(Random.nextBoolean()) getBestMove() else getRandomMove()
            "hard" -> getBestMove()
            else -> getBestMove()
        }

        return  bestMoveChance
    }

    /*

        0  50 90 100
        if (Random.nextInt(100) < bestMoveChance) {
            getBestMove()
        } else {
            getRandomMove()
        }
     */
//
    private fun getRandomMove(): Pair<Int, Int>? {
        val emptyCells = board.flatMapIndexed { i, row ->
            row.mapIndexedNotNull { j, cell -> if (cell == Player.NONE) Pair(i, j) else null }
        }
        return if (emptyCells.isNotEmpty()) emptyCells.random() else null
    }

    private fun getBestMove(): Pair<Int, Int>? {
        var bestScore = Int.MIN_VALUE
        var bestMove: Pair<Int, Int>? = null

        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j] == Player.NONE) {
                    board[i][j] = Player.O
                    val score = minimax(board, 0, isMaximizing = false)
                    board[i][j] = Player.NONE
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = Pair(i, j)
                    }
                }
            }
        }
        return bestMove
    }

    private fun minimax(board: Array<Array<Player>>, depth: Int, isMaximizing: Boolean): Int {
        val winner = checkWinner()
        if (winner == Player.O) return 10 - depth
        if (winner == Player.X) return depth - 10
        if (board.all { row -> row.all { it != Player.NONE } }) return 0

        return if (isMaximizing) {
            // AI’s turn
            var bestScore = Int.MIN_VALUE
            for (i in 0..2) {
                for (j in 0..2) {
                    if (board[i][j] == Player.NONE) {
                        board[i][j] = Player.O
                        val score = minimax(board, depth + 1, isMaximizing = false)
                        board[i][j] = Player.NONE
                        bestScore = maxOf(score, bestScore)
                    }
                }
            }
            bestScore
        } else {
            // Player’s turn
            var bestScore = Int.MAX_VALUE
            for (i in 0..2) {
                for (j in 0..2) {
                    if (board[i][j] == Player.NONE) {
                        board[i][j] = Player.X
                        val score = minimax(board, depth + 1, isMaximizing = true)
                        board[i][j] = Player.NONE
                        bestScore = minOf(score, bestScore)
                    }
                }
            }
            bestScore
        }
    }


    fun getWinningCells(): List<Pair<Int, Int>>? {
        // Check rows
        for (i in 0..2) {
            if (board[i][0] != Player.NONE &&
                board[i][0] == board[i][1] &&
                board[i][1] == board[i][2]) {
                return listOf(Pair(i, 0), Pair(i, 1), Pair(i, 2))
            }
        }

        // Check columns
        for (j in 0..2) {
            if (board[0][j] != Player.NONE &&
                board[0][j] == board[1][j] &&
                board[1][j] == board[2][j]) {
                return listOf(Pair(0, j), Pair(1, j), Pair(2, j))
            }
        }

        // Check first diagonal (top-left to bottom-right)
        if (board[0][0] != Player.NONE &&
            board[0][0] == board[1][1] &&
            board[1][1] == board[2][2]) {
            return listOf(Pair(0, 0), Pair(1, 1), Pair(2, 2))
        }

        // Check second diagonal (top-right to bottom-left)
        if (board[0][2] != Player.NONE &&
            board[0][2] == board[1][1] &&
            board[1][1] == board[2][0]) {
            return listOf(Pair(0, 2), Pair(1, 1), Pair(2, 0))
        }

        return null
    }
}