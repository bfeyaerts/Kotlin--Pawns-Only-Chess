package chess

const val FROM_COLUMN = "fromColumn"
const val FROM_ROW = "fromRow"
const val TO_COLUMN = "toColumn"
const val TO_ROW = "toRow"

const val PATTERN_MOVE = "(?<$FROM_COLUMN>[a-h])(?<$FROM_ROW>[1-8])(?<$TO_COLUMN>[a-h])(?<$TO_ROW>[1-8])"

const val INVALID_INPUT = "Invalid input"

enum class Command(val pattern: String){
    EXIT("exit"),
    MOVE(PATTERN_MOVE),
    ;

    val regex = pattern.toRegex()

    companion object {
        fun fromCommand(string: String): Command? {
            values().forEach {
                if (string.matches(it.regex))
                    return it
            }
            println("Invalid input")
            return null
        }
    }
}

val board = mutableSetOf<Pawn>()

data class Position(var column: Char, var row: Int) {
    fun move(deltaRows: Int): Position {
        return Position(column, row + deltaRows)
    }

    override fun toString(): String {
        return "$column$row"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Position

        if (column != other.column) return false
        if (row != other.row) return false

        return true
    }
}

enum class Color(val deltaRow: Int) {
    WHITE(1), // Moves up (row index increases)
    BLACK(-1), // Moves down (row index decreases)
    ;

    fun toChar(): Char {
        return name.first()
    }

    override fun toString(): String {
        return name.lowercase()
    }
}

class Pawn(val color: Color, var position: Position) {
    var initialPosition = true

    fun captureTargets(): Array<Position> {
        val toRow = position.row + color.deltaRow

        if (toRow !in 1..8)
            return emptyArray()

        val list = mutableListOf<Position>()
        if (position.column > 'a')
            list.add(Position(position.column - 1, toRow))
        if (position.column < 'h')
            list.add(Position(position.column + 1, toRow))
        return list.toTypedArray()
    }

    fun move(toPosition: Position) {
        position = toPosition
        initialPosition = false
    }

    override fun toString(): String {
        return "Pawn{color=$color, position=$position}"
    }
}

fun getPawn(position: Position): Pawn? {
    board.forEach { if (it.position == position) return it }
    return null
}

fun main() {
    println(" Pawns-Only Chess")

    for (column in 'a'..'h') {
        board.add(Pawn(Color.WHITE, Position(column, 2)))
        board.add(Pawn(Color.BLACK, Position(column, 7)))
    }

    println("First Player's name:")
    val player1 = readLine()!!
    println("Second Player's name:")
    val player2 = readLine()!!
    val players = arrayOf(player1, player2)

    printBoard()

    var currentPlayer = 0
    var eligibleForEnPassant: Pawn? = null

    while (true) {
        println("${players[currentPlayer]}'s turn:")
        val input = readLine()!!
        when (val command = Command.fromCommand(input)) {
            null -> continue
            Command.EXIT -> {
                println("Bye!")
                return
            }
            Command.MOVE -> {
                val matchResult = command.regex.find(input)

                val color = Color.values()[currentPlayer]
                val fromPosition = Position(matchResult!!.groups[FROM_COLUMN]!!.value.first(), matchResult!!.groups[FROM_ROW]!!.value.toInt())
                val pawn = getPawn(fromPosition)

                if (pawn == null || pawn.color != color) {
                    println("No $color pawn at $fromPosition")
                    continue
                }

                val toPosition = Position(matchResult!!.groups[TO_COLUMN]!!.value.first(), matchResult!!.groups[TO_ROW]!!.value.toInt())

                if (fromPosition == toPosition) {
                    println(INVALID_INPUT)
                    continue
                }
                val pawnAtToPosition = getPawn(toPosition)

                if (fromPosition.column == toPosition.column) {
                    if (pawnAtToPosition != null) {
                        println(INVALID_INPUT)
                        continue
                    }

                    val oneStep = fromPosition.move(pawn.color.deltaRow)
                    if (toPosition == oneStep) {
                        pawn.move(toPosition)
                        eligibleForEnPassant = null
                    } else {
                        val twoSteps = oneStep.move(pawn.color.deltaRow)
                        if (pawn.initialPosition && toPosition == twoSteps) {
                            pawn.move(toPosition)
                            eligibleForEnPassant = pawn
                        } else {
                            println(INVALID_INPUT)
                            continue
                        }
                    }
                } else if (toPosition in pawn.captureTargets()) {
                    if (pawnAtToPosition != null) {
                        if (pawnAtToPosition.color != color) {
                            board.remove(pawnAtToPosition)
                            pawn.move(toPosition)
                            eligibleForEnPassant = null
                        } else {
                            println(INVALID_INPUT)
                            continue
                        }
                    } else {
                        val enPassantPawn = getPawn(Position(toPosition.column, fromPosition.row))
                        if (enPassantPawn == eligibleForEnPassant) {
                            board.remove(enPassantPawn)
                            pawn.move(toPosition)
                            eligibleForEnPassant = null
                        } else {
                            println(INVALID_INPUT)
                            continue
                        }
                    }
                } else {
                    println(INVALID_INPUT)
                    continue
                }
                printBoard()
                if ((toPosition.row == 1) || (toPosition.row == 8) || board.none { it.color != color }) {
                    println("${color.name.first()}${color.name.lowercase().substring(1)} Wins!")
                    println("Bye!")
                    return
                }
            }
        }

        currentPlayer++
        currentPlayer %= 2

        // Check for stalemate
        val color = Color.values()[currentPlayer]
        val stalemate = board.filter { it.color == color }
            .none {
                val oneStep = it.position.move(color.deltaRow)
                if (getPawn(oneStep) == null)
                    return@none true
                if (it.initialPosition) {
                    val twoSteps = oneStep.move(color.deltaRow)
                    if (getPawn(twoSteps) == null)
                        return@none true
                }
                val canCapture = it.captureTargets()
                    .any {
                        val pawn = getPawn(it)
                        return@any pawn != null && pawn.color != color
                    }
                if (canCapture)
                    return@none true

                if (eligibleForEnPassant != null) {
                    if (eligibleForEnPassant.position.row == it.position.row
                        && Math.abs(eligibleForEnPassant.position.column - it.position.column) == 1)
                        return@none true
                }
                return@none false
            }
        if (stalemate) {
            println("Stalemate!")
            println("Bye!")
            return
        }
    }
}

fun printBoard() {
    for (row in 8 downTo 1) {
        println("  +---+---+---+---+---+---+---+---+")
        print("$row |")
        for (column in 'a' .. 'h') {
            print(" ${getPawn(Position(column, row))?.color?.toChar() ?: ' '} |")
        }
        println()
    }

    println("  +---+---+---+---+---+---+---+---+")
    println("    a   b   c   d   e   f   g   h")
}