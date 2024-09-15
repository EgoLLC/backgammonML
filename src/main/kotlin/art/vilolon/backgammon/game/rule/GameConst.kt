package art.vilolon.backgammon.game.rule

import art.vilolon.backgammon.game.entity.GAvailableMoves
import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GDice
import art.vilolon.backgammon.game.entity.GDices
import art.vilolon.backgammon.game.entity.GDicesOnBoard
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GGameState
import art.vilolon.backgammon.game.entity.GPlayer
import art.vilolon.backgammon.game.entity.GVS.*
import art.vilolon.backgammon.game.entity.HolePosition
import kotlin.random.Random

const val COMPUTER_EASY = 1
const val COMPUTER_NORMAL = 2
const val COMPUTER_HARD = 3

const val P1 = 1
const val P1_HEAD = 1
const val P1_END = P1_HEAD
const val P2 = 2
const val P2_HEAD = 13
const val P2_END = P2_HEAD
const val P_CHECKERS_COUNT = 15
const val BOARD_HOLE_COUNT = 24
val P1_CHECKER_IDS = 0..14
val P2_CHECKER_IDS = 15..29
val TOP_ROW = 13..24
val BOTTOM_ROW = 1..12
val P1_HOME_POSITIONS = 19..24
val P2_HOME_POSITIONS = 7..12
val RIGHT_BOARD = 7..18
internal const val P1_BOARD_OUT = 24
internal const val P2_JUMP_BOARD = 24
internal const val P2_BOARD_OUT = 12
internal const val BLOCK_LINE_LENGTH = 6
internal const val GAME_HOLE_LENGTH = P_CHECKERS_COUNT * BOARD_HOLE_COUNT

val NEW_GAME_P2AI: GGame
    get() = GGame(
        state = GGameState.PLAYING,
        turnPlayer = Random.nextInt(2) + 1,
        vs = AI
    )

val NEW_GAME_P2P: GGame
    get() = GGame(
        player1 = GPlayer(
            checkers = buildList {
                add(GChecker(0,  1, 0, true, false))
                add(GChecker(1,  1, 1, true, false))
                add(GChecker(2,  3, 0, true, false))
                add(GChecker(3,  11, 0, true, false))
                add(GChecker(4,  15, 0, true, false))
                add(GChecker(5,  20, 0, true, false))
                add(GChecker(6,  20, 1, true, false))
                add(GChecker(7,  20, 2, true, false))
                add(GChecker(8,  20, 3, true, false))
                add(GChecker(9,  20, 4, true, false))
                add(GChecker(10, 20, 5, true, false))
                add(GChecker(11, 21, 0, true, false))
                add(GChecker(12, 23, 0, true, false))
                add(GChecker(13, 23, 1, true, false))
                add(GChecker(14, 23, 0, true, false))
            }
        ),
        player2 = GPlayer(
            checkers = buildList {
                add(GChecker(15, 2, 0, true, false))
                add(GChecker(16, 4, 0, true, false))
                add(GChecker(17, 5, 0, true, false))
                add(GChecker(18, 5, 1, true, false))
                add(GChecker(19, 6, 0, true, false))
                add(GChecker(20, 7, 0, true, false))
                add(GChecker(21, 8, 0, true, false))
                add(GChecker(22, 9, 0, true, false))
                add(GChecker(23, 10, 0, true, false))
                add(GChecker(24, 10, 1, true, false))
                add(GChecker(25, 10, 2, true, false))
                add(GChecker(26, 17, 0, true, false))
                add(GChecker(27, 19, 0, true, false))
                add(GChecker(28, 22, 0, true, false))
                add(GChecker(29, 24, 0, true, false))
            }
        ),
        state = GGameState.PLAYING,
        turnPlayer = P1,
        dices = GDicesOnBoard(
            leftBoard = GDices(
                listOf(
                    GDice(1, true, 12),
                    GDice(2, false, 13)
                )
            )
        ),
        vs = Person)

val NEW_GAME_P2P_REMOTE: GGame
    get() = GGame(vs = Remote("room_id"))

val NO_DICES: GDices
    get() = GDices()

val NO_AVAILABLE_MOVES: GAvailableMoves
    get() = GAvailableMoves()

fun HolePosition.normalizeP2Position(): HolePosition = when (this) {
    in TOP_ROW -> this - P2_HEAD + 1
    in BOTTOM_ROW -> this + P2_HEAD - 1
    else -> throw IllegalStateException("Wrong position:$this")
}