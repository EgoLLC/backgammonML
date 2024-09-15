package art.vilolon.backgammon.ml.mappers

import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.rule.BOARD_HOLE_COUNT
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.NEW_GAME_P2AI
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.utils.LOGGER_FACTORY
import art.vilolon.backgammon.game.utils.Logger
import art.vilolon.backgammon.ml.model.Input
import art.vilolon.backgammon.ml.model.Output
import kotlin.random.Random

class Mapper(
    private val gameRule: GameRule = GameRule(LOGGER_FACTORY),
) {

    /**
     * @param game to model date
     * */
    fun toInput(game: GGame): Input {

        val p1Checker2Position: Array<FloatArray> = game.player2.checkers.map { checker ->
            FloatArray(BOARD_HOLE_COUNT) { index ->
                if (checker.position - 1 == index) 1f else 0f
            }
        }.toTypedArray()

        val p2Checker2Position: Array<FloatArray> = game.player1.checkers.map { checker ->
            FloatArray(BOARD_HOLE_COUNT) { index ->
                if (checker.position - 1 == index) 1f else 0f
            }
        }.toTypedArray()

        val movesAndCheckersForPlayer = gameRule.getMovesAndCheckersForPlayer(
            player = if (game.turnPlayer == P1) game.player1 else game.player2,
            opponent = if (game.turnPlayer == P1) game.player2 else game.player1,
            allDices = game.dices.values,
            turnPlayer = game.turnPlayer,
        )

        val movesVariants: Array<FloatArray> = Array(P_CHECKERS_COUNT) { checker ->
            val checkerId = if (game.turnPlayer == P1) checker else checker + P_CHECKERS_COUNT
            val checkChecker = game.player1.checkers.find { it.id == checkerId }
                ?: game.player2.checkers.find { it.id == checkerId }!!
            val checkerWithMove = movesAndCheckersForPlayer.keys.find { it.position == checkChecker.position }

            FloatArray(BOARD_HOLE_COUNT) { hole ->
                if (checkerWithMove != null) {
                    if (movesAndCheckersForPlayer[checkerWithMove]?.any { it.toPosition == hole + 1 } == true) 1f else 0f
                } else {
                    0f
                }
            }
        }

        return Input(
            p1Checker2Position,
            p2Checker2Position,
            movesVariants,
        )
    }

    /**
     * @param result [FloatArray] is model output optimal move value, size of 360 items
     * */
    fun toOutput(result: FloatArray): Output {
        val maxValue = result.max()
        val maxValueIndex = result.indexOfFirst { maxValue == it }

        val checkerId = maxValueIndex / BOARD_HOLE_COUNT
        val toHolePosition = maxValueIndex % BOARD_HOLE_COUNT + 1

        return Output(checkerId, toHolePosition, 0, 0, 0)
    }

    /**
     * @param result is model output optimal move value, of 360 items
     * */
    fun toOutput(result: Int): Output {

        val checkerId = result / BOARD_HOLE_COUNT
        val toHolePosition = result % BOARD_HOLE_COUNT + 1

        return Output(checkerId, toHolePosition, 0, 0,0)
    }

    /**
     * @param output is not model output optimal move value,
     * @return [FloatArray] is true model output, size 360 items
     * */
    fun toModelOutput(output: Output): FloatArray {
        return FloatArray(360) {
            if (output.checkerId * BOARD_HOLE_COUNT + output.to - 1 == it) 1f else 0f
        }
    }
}


fun Array<*>.flattenFloats(): FloatArray {
    val result = mutableListOf<Float>()

    fun flatten(array: Any?): Unit = when (array) {
        is FloatArray -> array.forEach { result.add(it) }
        is Array<*> -> array.forEach { flatten(it) }
        else -> throw IllegalArgumentException("Cannot flatten object: '$array'")
    }

    flatten(this)

    return result.toFloatArray()
}