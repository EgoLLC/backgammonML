package art.vilolon.backgammon.ai

import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GMovePosition
import art.vilolon.backgammon.game.entity.GPlayer
import art.vilolon.backgammon.game.entity.HolePosition
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P1_END
import art.vilolon.backgammon.game.rule.P1_HOME_POSITIONS
import art.vilolon.backgammon.game.rule.P2_HEAD
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.rule.normalizeP2Position
import art.vilolon.backgammon.game.utils.Logger

private const val TAG = "AI2"

class AI2 constructor(
    private val gameRule: GameRule,
    loggerFactory: Logger.Factory,
) : AI {

    private val logger: Logger = loggerFactory.create(TAG)

    private var favoriteMove: FavoriteMove? = null

    private class FavoriteMove(
        val cost: Int,
        val checker: GChecker,
        val moveTo: HolePosition,
    ) {
        override fun toString(): String {
            return "FavoriteMove(cost=$cost, from=${checker.position}, to=$moveTo)"
        }
    }

    override suspend fun getSelectChecker(game: GGame): GChecker? {
        check(game.turnPlayer == P1)
        val checkers2Moves = gameRule.getMovesAndCheckersForPlayer(
            player = game.player1,
            opponent = game.player2,
            allDices = game.dices.values,
            turnPlayer = game.turnPlayer,
        )
        favoriteMove = getOptimalMove(
            checkers2Moves = checkers2Moves,
            ai = game.player1,
            player = game.player2,
        )
        logger.d("AI2 select: ${favoriteMove?.checker}")
        return favoriteMove?.checker
    }

    override suspend fun getMovePosition(game: GGame): HolePosition? {
        if (game.availableMoves.holes.isEmpty()) return null
        check(game.turnPlayer == P1)
        val move = favoriteMove?.moveTo
        if (move == null) {
            val selectChecker = getSelectChecker(game)
            logger.d("AI2 call recursive checker: $selectChecker")
            return getMovePosition(game)
        }
        logger.d("AI2 Move from:${favoriteMove?.checker?.position} to:$move")
        favoriteMove = null
        return move
    }

    private fun getOptimalMove(
        checkers2Moves: Map<GChecker, Set<GMovePosition>>,
        ai: GPlayer,
        player: GPlayer,
    ): FavoriteMove? {
        val favoriteMoves: MutableList<FavoriteMove> = mutableListOf()

        for ((from, moves) in checkers2Moves) {

            if (favoriteMoves.any { it.checker.position == from.position }) {
                continue
            }

            moves.forEach { move ->
                var moveCost = 0
                val moveToPosition = move.toPosition

                moveCost -= ((move.dices.size - 1) * 4)
                    .also {
                        if (it > 0) logger.v("Decrease by use dices: -$it")
                    }

                val playerLastPosition = player.checkers.minOf { it.position.normalizeP2Position() }
                val isPlayerThrowToHead = player.allAtHome && player.checkers.any { it.position == P2_HEAD }
                val isPlayerWentFar = playerLastPosition > 8
                val isNotDisturbTo = isNotDisturb(moveToPosition, playerLastPosition) || isPlayerThrowToHead

                if (isNotDisturbTo && !isPlayerWentFar) {
                    moveCost = -8

                    if (moveToPosition in P1_HOME_POSITIONS && move.checker.position !in P1_HOME_POSITIONS) {
                        moveCost += 2
                    }

                    logger.v("Move to don't disturb, cost: $moveCost, playerLastPosition: ${playerLastPosition.normalizeP2Position()}, toPosition: $moveToPosition")
                }

                val oneOnPosition = ai.checkers.count { it.position == from.position } == 1
                val takeFreeHole = !(ai.checkers.any { it.position == moveToPosition })
                val maxPosition = ai.checkers.maxOf { it.position }
                val lastCheckerPosition = ai.checkers.minBy { it.position }.position
                val isFurthest = maxPosition <= moveToPosition
                val middlePosition = ai.checkers.sumOf { it.position } / P_CHECKERS_COUNT
                val isNotDisturbFrom = isNotDisturb(move.checker.position, playerLastPosition) || isPlayerThrowToHead

                if (isNotDisturbFrom && !isPlayerWentFar) {
                    moveCost += (4).also {
                        logger.v("Move from don't disturb, +$it, playerLastPosition: ${playerLastPosition.normalizeP2Position()}, toPosition: $moveToPosition")
                    }
                    if (!isNotDisturbTo && takeFreeHole) {
                        moveCost += (4).also {
                            logger.v("Move from don't disturb to disturb, +$it, playerLastPosition: ${playerLastPosition.normalizeP2Position()}, toPosition: $moveToPosition")
                        }
                    }
                }

                if (takeFreeHole) {
                    moveCost += (1).also {
                        logger.v("Add $it by move to free hole")
                    }
                }

                // check ai block line
                var i = 1
                while (true) {
                    if (
                        !isNotDisturbTo
                        && takeFreeHole
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && (checker != from && oneOnPosition)
                                    && checker.position + i == moveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by block line +")
                    }
                }
                i = 1
                while (true) {
                    if (
                        !isNotDisturbTo
                        && takeFreeHole
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && (checker != from && oneOnPosition)
                                    && checker.position - i == moveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by block line -")
                    }
                }

                // check don't destroy AI block line
                i = 1
                while (true) {
                    if (
                        !isNotDisturbFrom
                        && oneOnPosition
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && checker.position + i == moveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by destroy block line +")
                    }
                }
                i = 1
                while (true) {
                    if (
                        !isNotDisturbFrom
                        && oneOnPosition
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && checker.position - i == moveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by destroy block line -")
                    }
                }

                // check enemy block line
                i = 1
                while (true) {
                    if (
                        (!isFurthest || move.checker.position in P1_HOME_POSITIONS)
                        && takeFreeHole
                        && player.checkers.any { it.position + i == moveToPosition }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by destroy enemy block line +")
                    }
                }

                i = 1
                while (true) {
                    if (
                        (!isFurthest || move.checker.position in P1_HOME_POSITIONS)
                        && takeFreeHole
                        && player.checkers.any { it.position - i == moveToPosition }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                i -= 1
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by destroy enemy block line -")
                    }
                }

                val isLastChecker = lastCheckerPosition == move.checker.position

                if (isLastChecker) {
                    moveCost += 3.also {
                        logger.v("Add $it by last checker")
                    }
                }

                // check from enemy block line
                i = 1
                var enemyBlockLineLength = 0
                while (true) {
                    if (!isLastChecker
                        && oneOnPosition
                        && player.checkers.any { it.position + i == from.position }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    enemyBlockLineLength += i
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by undo destroy enemy block line +")
                    }
                }
                i = 1
                while (true) {
                    if (!isLastChecker
                        && oneOnPosition
                        && player.checkers.any { it.position - i == from.position }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    enemyBlockLineLength += i
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by undo destroy enemy block line -")
                    }
                }

                if (enemyBlockLineLength >= 5) {
                    moveCost -= (5).also {
                        logger.v("Decrease -$it by enemy block line")
                    }
                }

                // check the furthest move
                i = 1
                while (true) {
                    if (ai.checkers.any { checker ->
                            val checkerPosition = checker.position
                            move.toPosition in P1_HOME_POSITIONS
                                    || checkerPosition - i == moveToPosition
                                    || checkerPosition + i == moveToPosition
                                    || checkerPosition == moveToPosition
                        }
                    ) {
                        break
                    } else {
                        i++
                    }
                }
                i -= 1
                if (i > 0) {
                    moveCost += (i * .5f).toInt().also {
                        logger.v("Add $it by furthest move")
                    }
                }

                moveCost += (((middlePosition - moveToPosition) * .3f).toInt()).also {
                    logger.v("By middle position cost $it, middlePosition:${middlePosition}")
                }

                if (from.position !in P1_HOME_POSITIONS && move.toPosition in P1_HOME_POSITIONS) {

                    val useDicesComeHomePoints = when (move.dices.size) {
                        1 -> 4
                        2 -> 3
                        3 -> 2
                        else -> 1
                    }

                    moveCost += (useDicesComeHomePoints).also {
                        logger.v("Add $it by come home")
                    }
                }

                moveCost += ((ai.checkers.count { it.position == from.position } - 1) * 1.3f).toInt()
                    .also {
                        if (it > 0) logger.v("Add by take head:$it")
                    }

                if (!ai.allAtHome && move.toPosition != P1_END) {

                    val notComeHomeCoefficient = if (
                        move.toPosition in P1_HOME_POSITIONS
                        && from.position !in P1_HOME_POSITIONS
                    ) 1 else 2

                    moveCost -= (ai.checkers.count { it.position == move.toPosition } * notComeHomeCoefficient)
                        .also {
                            if (it > 0) logger.v("Decrease by add to head: -$it")
                        }
                }

                if (from.position in P1_HOME_POSITIONS) {
                    moveCost -= (2).also {
                        logger.v("Decrease -$it by move on home")
                    }
                    if (!ai.allAtHome) {
                        moveCost -= (2).also {
                            logger.v("Decrease -$it by move not all at home")
                        }
                    }
                }

                if (ai.allAtHome && move.toPosition == P1_END) {
                    val comeHomeWithDicesPoints = when (move.dices.size) {
                        1 -> 8
                        2 -> 2
                        3 -> 1
                        else -> 0
                    }
                    moveCost += (comeHomeWithDicesPoints).also {
                        logger.v("Add $it by reach end")
                    }
                }

                favoriteMoves.add(
                    FavoriteMove(
                        cost = moveCost,
                        checker = from,
                        moveTo = move.toPosition
                    ).also {
                        logger.v(it.toString())
                        logger.v("====================")
                    }
                )
            }
        }
        return favoriteMoves.maxByOrNull { it.cost }
            .also {
                favoriteMoves.sortedByDescending { it.cost }.forEachIndexed { index, favoriteMove ->
                    logger.v("$index) $favoriteMove")
                }
            }
    }

    private fun isNotDisturb(toPosition: HolePosition, lastPlayerPosition: HolePosition): Boolean {
        return toPosition.normalizeP2Position() < lastPlayerPosition
    }
}
