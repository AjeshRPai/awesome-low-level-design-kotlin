package questions.tictactoe

import java.util.*

data class Player(
    var name: String,
    var symbol: Char
)

class Board(
    val grid: Array<CharArray> = Array(3) { CharArray(3) { '-' } },
    var movesCount: Int = 0
) {

    fun makeMove(row: Int, col: Int, symbol: Char): Boolean {
        synchronized(this) {
            if (row in 0..2 && col in 0..2 && grid[row][col] == '-') {
                grid[row][col] = symbol
                movesCount++
                return true
            }
            return false
        }
    }

    fun isFull(): Boolean {
        return movesCount == 9
    }

    fun hasWinner(): Boolean {
        // Check rows
        for (row in 0..2) {
            if (grid[row][0] != '-' && grid[row][0] == grid[row][1] && grid[row][1] == grid[row][2]) {
                return true
            }
        }

        // Check columns
        for (col in 0..2) {
            if (grid[0][col] != '-' && grid[0][col] == grid[1][col] && grid[1][col] == grid[2][col]) {
                return true
            }
        }

        // Check diagonals
        if (grid[0][0] != '-' && grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2]) {
            return true
        }
        return grid[0][2] != '-' && grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0]
    }

    fun printBoard() {
        for (row in grid) {
            println(row.joinToString(" | ") { it.toString() })
            println("-".repeat(9))
        }
        println()
    }
}

class Game(private val player1: Player, private val player2: Player) {
    private val board = Board()
    private var currentPlayer: Player = player1

    fun play() {
        board.printBoard()

        while (!board.isFull()) {
            println("${currentPlayer.name}'s turn.")
            val row = getValidInput("Enter row (0-2): ")
            val col = getValidInput("Enter column (0-2): ")

            if (board.makeMove(row, col, currentPlayer.symbol)) {
                board.printBoard()
                if (board.hasWinner()) {
                    println("${currentPlayer.name} wins!")
                    return // Terminate the game after a win
                }
                switchPlayer()
            } else {
                println("Invalid move, try again.")
            }
        }

        // If loop exits because the board is full
        if (board.isFull() && !board.hasWinner()) {
            println("It's a draw!")
        }
    }

    private fun switchPlayer() {
        currentPlayer = if (currentPlayer == player1) player2 else player1
    }

    private fun getValidInput(message: String): Int {
        val scanner = Scanner(System.`in`)
        while (true) {
            print(message)
            if (scanner.hasNextInt()) {
                val input = scanner.nextInt()
                if (input in 0..2) {
                    return input
                }
            } else {
                scanner.next()
            }
            println("Invalid input! Please enter a number between 0 and 2.")
        }
    }
}

fun main() {
    val player1 = Player("Player 1", 'X')
    val player2 = Player("Player 2", 'O')

    val game = Game(player1, player2)
    game.play()
}
