package questions.chessGame

import java.util.*
import kotlin.math.abs


abstract class Piece {
    abstract var color: Color
    abstract var currentRow: Int
    abstract var currentColumn: Int

    abstract fun canMove(board: Board, destRow: Int, destCol: Int): Boolean
}

enum class Color(int: Int) {
    WHITE(0),
    BLACK(1)
}

class King(
    override var color: Color,
    override var currentRow: Int,
    override var currentColumn: Int
) : Piece() {
    override fun canMove(board: Board, destRow: Int, destCol: Int): Boolean {
        val rowDiff: Int = abs(destRow - currentRow)
        val colDiff: Int = abs(destCol - currentColumn)
        return (colDiff<=1 && rowDiff<=1)
    }
}

class Bishop(override var color: Color, override var currentRow: Int, override var currentColumn: Int) : Piece() {
    override fun canMove(board: Board, destRow: Int, destCol: Int): Boolean {
        val rowDiff: Int = abs(destRow - currentRow)
        val colDiff: Int = abs(destCol - currentColumn)
        return (rowDiff == colDiff)
    }
}

class Knight(override var color: Color, override var currentRow: Int, override var currentColumn: Int) : Piece() {
    override fun canMove(board: Board,destRow: Int, destCol: Int): Boolean {
        val rowDiff: Int = Math.abs(destRow - currentRow)
        val colDiff: Int = Math.abs(destCol - currentColumn)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }
}

class Pawn(override var color: Color, override var currentRow: Int, override var currentColumn: Int) : Piece() {
    override fun canMove(board: Board, destRow: Int, destCol: Int): Boolean {
        val rowDiff: Int = destRow - currentRow
        val colDiff: Int = Math.abs(destCol - currentColumn)

        return if (color == Color.WHITE) {
            (rowDiff == 1 && colDiff == 0) ||
                    (currentRow == 1 && rowDiff == 2 && colDiff == 0) ||
                    (rowDiff == 1 && colDiff == 1 && board.getPiece(destRow, destCol) != null)
        } else {
            (rowDiff == -1 && colDiff == 0) ||
                    (currentRow == 6 && rowDiff == -2 && colDiff == 0) ||
                    (rowDiff == -1 && colDiff == 1 && board.getPiece(destRow, destCol) != null)
        }
    }
}

class Queen(override var color: Color, override var currentRow: Int, override var currentColumn: Int) : Piece() {
    override fun canMove(board: Board, destRow: Int, destCol: Int): Boolean {
        val rowDiff: Int = abs(destRow - currentRow)
        val colDiff: Int = abs(destCol - currentColumn)
        return (rowDiff == colDiff) || (currentRow == destRow || currentColumn == destCol)
    }
}

class Rook(override var color: Color, override var currentRow: Int, override var currentColumn: Int) : Piece() {
    override fun canMove(board: Board, destRow: Int, destCol: Int): Boolean {
        return (currentRow == destRow || currentColumn == destCol)
    }
}


class Board {
    private val board = Array(8) { arrayOfNulls<Piece>(8) }

    init {
        initializeBoard()
    }

    private fun initializeBoard() {
        // Initialize white pieces
        board[0][0] = Rook(Color.WHITE, 0, 0)
        board[0][1] = Knight(Color.WHITE, 0, 1)
        board[0][2] = Bishop(Color.WHITE, 0, 2)
        board[0][3] = Queen(Color.WHITE, 0, 3)
        board[0][4] = King(Color.WHITE, 0, 4)
        board[0][5] = Bishop(Color.WHITE, 0, 5)
        board[0][6] = Knight(Color.WHITE, 0, 6)
        board[0][7] = Rook(Color.WHITE, 0, 7)
        for (i in 0..7) {
            board[1][i] = Pawn(Color.WHITE, 1, i)
        }

        // Initialize black pieces
        board[7][0] = Rook(Color.BLACK, 7, 0)
        board[7][1] = Knight(Color.BLACK, 7, 1)
        board[7][2] = Bishop(Color.BLACK, 7, 2)
        board[7][3] = Queen(Color.BLACK, 7, 3)
        board[7][4] = King(Color.BLACK, 7, 4)
        board[7][5] = Bishop(Color.BLACK, 7, 5)
        board[7][6] = Knight(Color.BLACK, 7, 6)
        board[7][7] = Rook(Color.BLACK, 7, 7)
        for (i in 0..7) {
            board[6][i] = Pawn(Color.BLACK, 6, i)
        }
    }

    fun getPiece(row: Int, col: Int): Piece? {
        return board[row][col]
    }

    fun setPiece(row: Int, col: Int, piece: Piece?) {
        board[row][col] = piece
    }

    fun isValidMove(piece: Piece?, destRow: Int, destCol: Int): Boolean {
        if (piece == null || destRow < 0 || destRow > 7 || destCol < 0 || destCol > 7) {
            return false
        }
        val destPiece = board[destRow][destCol]
        return (destPiece == null || destPiece.color != piece.color) &&
                piece.canMove(this, destRow, destCol)
    }

    fun isCheckmate(color: Color?): Boolean {
        // TODO: Implement checkmate logic
        return false
    }

    fun isStalemate(color: Color?): Boolean {
        // TODO: Implement stalemate logic
        return false
    }
}


class ChessGame {
    private val board = Board()
    private val players = arrayOf(Player(Color.WHITE), Player(Color.BLACK))
    private var currentPlayer = 0

    fun start() {
        // Game loop
        while (!isGameOver) {
            val player = players[currentPlayer]
            println("$player.color 's turn.")

            // Get move from the player
            val move: Move = getPlayerMove(player)

            // Make the move on the board
            try {
                player.makeMove(board, move)
            } catch (e: InvalidMoveException) {
                System.out.println(e.message)
                println("Try again!")
                continue
            }

            // Switch to the next player
            currentPlayer = (currentPlayer + 1) % 2
        }

        // Display game result
        displayResult()
    }

    private val isGameOver: Boolean
        get() = board.isCheckmate(players[0].color) || board.isCheckmate(players[1].color) ||
                board.isStalemate(players[0].color) || board.isStalemate(players[1].color)

    private fun getPlayerMove(player: Player): Move {
        // TODO: Implement logic to get a valid move from the player
        // For simplicity, let's assume the player enters the move via console input
        val scanner: Scanner = Scanner(System.`in`)
        print("Enter source row: ")
        val sourceRow: Int = scanner.nextInt()
        print("Enter source column: ")
        val sourceCol: Int = scanner.nextInt()
        print("Enter destination row: ")
        val destRow: Int = scanner.nextInt()
        print("Enter destination column: ")
        val destCol: Int = scanner.nextInt()

        val piece = board.getPiece(sourceRow, sourceCol)
        require(!(piece == null || piece.color != player.color)) { "Invalid piece selection!" }

        return Move(piece, destRow, destCol)
    }

    private fun displayResult() {
        if (board.isCheckmate(Color.WHITE)) {
            println("Black wins by checkmate!")
        } else if (board.isCheckmate(Color.BLACK)) {
            println("White wins by checkmate!")
        } else if (board.isStalemate(Color.WHITE) || board.isStalemate(Color.BLACK)) {
            println("The game ends in a stalemate!")
        }
    }
}

class Player(val color: Color) {
    fun makeMove(board: Board, move: Move) {
        val piece: Piece = move.piece
        val destRow: Int = move.destRow
        val destCol: Int = move.destCol

        if (board.isValidMove(piece, destRow, destCol)) {
            val sourceRow: Int = piece.currentRow
            val sourceCol: Int = piece.currentColumn
            board.setPiece(sourceRow, sourceCol, null)
            board.setPiece(destRow, destCol, piece)
            piece.currentRow  = destRow
            piece.currentColumn = destCol
        } else {
            throw InvalidMoveException("Invalid move!")
        }
    }
}

class InvalidMoveException(message: String?) : RuntimeException(message)

class Move(val piece: Piece, val destRow: Int, val destCol: Int)

fun main(){
    val chessGame = ChessGame()
    chessGame.start()
}