package art.vilolon.backgammon.game.entity

import art.vilolon.backgammon.game.rule.NO_AVAILABLE_MOVES
import art.vilolon.backgammon.game.rule.NO_DICES
import art.vilolon.backgammon.game.rule.P2
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT

data class GGame(
    val vs: GVS,
    val player1: GPlayer = GPlayer(
        checkers = GChecker.getForPlayer1()
    ),
    val player2: GPlayer = GPlayer(
        checkers = GChecker.getForPlayer2()
    ),
    val dices: GDicesOnBoard = GDicesOnBoard(),
    val availableMoves: GAvailableMoves = NO_AVAILABLE_MOVES,
    val turnPlayer: Int = P2,
    val state: GGameState = GGameState.START,
): java.io.Serializable

data class GPlayer(
    val checkers: List<GChecker>,
    val tookHead: Boolean = false,
    val allAtHome: Boolean = false,
): java.io.Serializable

data class GChecker(
    val id: Int,
    val position: HolePosition,
    val highPosition: Int,
    val canMove: Boolean = true,
    val isSelected: Boolean = false,
): java.io.Serializable {
    companion object {
        fun getForPlayer1() = buildList {
            repeat(P_CHECKERS_COUNT) { id ->
                add(
                    GChecker(
                        id = id,
                        position = 1,
                        highPosition = id,
                    )
                )
            }
        }

        fun getForPlayer2() = buildList {
            repeat(P_CHECKERS_COUNT) { id ->
                add(
                    GChecker(
                        id = P_CHECKERS_COUNT + id,
                        position = 13,
                        highPosition = id,
                    )
                )
            }
        }
    }
}

data class GDicesOnBoard(
    val leftBoard: GDices = NO_DICES,
    val rightBoard: GDices = NO_DICES,
): java.io.Serializable {
    val values: List<GDice>
        get() = when {
            leftBoard.values.isNotEmpty() -> leftBoard.values
            rightBoard.values.isNotEmpty() -> rightBoard.values
            else -> emptyList()
        }
}

data class GDices(
    val values: List<GDice> = emptyList(),
    val rolling: Boolean = false,
): java.io.Serializable

data class GDice(
    val value: Int,
    val isUsed: Boolean = false,
    val id: Long = _id++,
): java.io.Serializable {
    companion object {
        private var _id = 0L
    }
}

data class GAvailableMoves(
    val holes: Set<GMovePosition> = emptySet(),
): java.io.Serializable

enum class GGameState: java.io.Serializable {
    START,
    PLAYING,
    END,
}

sealed interface GVS: java.io.Serializable {

    object AI : GVS {
        private fun readResolve(): Any = AI
    }

    object Person : GVS {
        private fun readResolve(): Any = Person
    }

    data class Remote(val id: String) : GVS
}

data class GMovePosition(
    val checker: GChecker,
    val toPosition: HolePosition,
    val dices: List<GDice>,
): java.io.Serializable

typealias HolePosition = Int

val HolePosition.mirrorPosition: HolePosition get() = 25 - this
