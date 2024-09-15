package art.vilolon.backgammon.ai

import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GMovePosition
import art.vilolon.backgammon.game.entity.GPlayer
import art.vilolon.backgammon.game.entity.HolePosition
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.P2_END
import art.vilolon.backgammon.game.rule.P2_HOME_POSITIONS
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.rule.normalizeP2Position
import art.vilolon.backgammon.game.utils.Logger

private const val TAG = "AI2P1"

class AI2P2 constructor(
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
        val checkers2Moves = gameRule.getMovesAndCheckersForPlayer(
            player = game.player2,
            opponent = game.player1,
            turnPlayer = game.turnPlayer,
            allDices = game.dices.values,
        )
        favoriteMove = getOptimalMove(
            checkers2Moves = checkers2Moves,
            ai = game.player2,
            player = game.player1
        )
        logger.d("AI2 selectChecker: ${favoriteMove?.checker}")
        return favoriteMove?.checker
    }

    override suspend fun getMovePosition(game: GGame): HolePosition? {
        if (game.availableMoves.holes.isEmpty()) return null
        val move = favoriteMove?.moveTo
        if (move == null) {
            val selectChecker = getSelectChecker(game)
            logger.d("AI2 call recursive checker: $selectChecker")
            return getMovePosition(game)
        }
        logger.d("AI2 Checker:${favoriteMove?.checker?.position} moveTo: $move")
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

                moveCost -= ((move.dices.size - 1) * 4)
                    .also {
                        if (it > 0) logger.v("Decrease by use dices: -$it")
                    }

                val playerLastPosition = player.checkers.minOf { it.position } // p1 not normalize
                val isNotDisturb = isNotDisturb(move.toPosition, playerLastPosition)

                if (isNotDisturb) {
                    moveCost = -8

                    if (move.toPosition in P2_HOME_POSITIONS && move.checker.position !in P2_HOME_POSITIONS) {
                        moveCost += 2
                    }

                    logger.v("Don't disturb: $moveCost")
                }

                val oneOnPosition = ai.checkers.count { it.position == from.position } == 1
                val takeFreeHole = ai.checkers.any { it.position != move.toPosition }
                val normalizeFirstPosition = ai.checkers.maxOf { it.position.normalizeP2Position() }.normalizeP2Position()
                val lastCheckerPosition = ai.checkers.minBy { it.position.normalizeP2Position() }.position
                val isFurthest = normalizeFirstPosition >= move.toPosition.normalizeP2Position()
                val isFreeHole = ai.checkers.count { it.position == move.toPosition } == 0
                val normalizeMiddlePosition = ai.checkers.sumOf { it.position.normalizeP2Position() } / P_CHECKERS_COUNT

                if (isFreeHole) {
                    moveCost += (1).also {
                        logger.v("Add $it by move to free hole")
                    }
                }

                // check ai block line
                var i = 1
                val normalizeMoveToPosition = move.toPosition.normalizeP2Position()
                while (true) {
                    if (
                        !isNotDisturb
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && (checker != from && oneOnPosition)
                                    && checker.position.normalizeP2Position() + i == normalizeMoveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by block line +")
                    }
                }
                i = 1
                while (true) {
                    if (
                        !isNotDisturb
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && (checker != from && oneOnPosition)
                                    && checker.position.normalizeP2Position() - i == normalizeMoveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by block line -")
                    }
                }

                // check don't destroy AI block line
                i = 1
                while (true) {
                    if (
                        !isNotDisturb
                        && oneOnPosition
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && checker.position.normalizeP2Position() + i == normalizeMoveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by destroy block line +")
                    }
                }
                i = 1
                while (true) {
                    if (
                        !isNotDisturb
                        && oneOnPosition
                        && ai.checkers.any { checker ->
                            !isNotDisturb(checker.position, playerLastPosition)
                                    && checker.position.normalizeP2Position() - i == normalizeMoveToPosition
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by destroy block line -")
                    }
                }

                // check enemy block line
                i = 1
                while (true) {
                    if (
                        (!isFurthest || move.checker.position in P2_HOME_POSITIONS)
                        && isFreeHole
                        && takeFreeHole && player.checkers.any { it.position.normalizeP2Position() + i == normalizeMoveToPosition }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost += (i * 2).also {
                        logger.v("Add $it by destroy enemy block line +")
                    }
                }

                i = 1
                while (true) {
                    if (
                        (!isFurthest || move.checker.position in P2_HOME_POSITIONS)
                        && isFreeHole
                        && takeFreeHole && player.checkers.any { it.position.normalizeP2Position() - i == normalizeMoveToPosition }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
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
                while (true) {
                    if (!isLastChecker
                        && oneOnPosition
                        && player.checkers.any {
                            it.position.normalizeP2Position() + i == from.position.normalizeP2Position()
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by undo destroy enemy block line +")
                    }
                }
                i = 1
                while (true) {
                    if (!isLastChecker
                        && oneOnPosition
                        && player.checkers.any {
                            it.position.normalizeP2Position() - i == from.position.normalizeP2Position()
                        }
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                if (i > 1) {
                    moveCost -= (i * 2).also {
                        logger.v("Decrease -$it by undo destroy enemy block line -")
                    }
                }

                // check the furthest move
                i = 1
                while (true) {
                    if (ai.checkers.any { checker ->
                            val checkerPosition = checker.position.normalizeP2Position()
                            move.toPosition in P2_HOME_POSITIONS
                                    || checkerPosition - i == normalizeMoveToPosition
                                    || checkerPosition + i == normalizeMoveToPosition
                                    || checkerPosition == normalizeMoveToPosition
                        }
                    ) {
                        break
                    } else {
                        i++
                    }
                }
                moveCost += (i * .5f).toInt().also {
                    if (it > 0) logger.v("Add $it by furthest move")
                }



                moveCost += (((normalizeMiddlePosition - normalizeMoveToPosition) * .3f).toInt()).also {
                    logger.v("By middle position move:$it middlePosition:${normalizeMiddlePosition.normalizeP2Position()}")
                }

                if (from.position !in P2_HOME_POSITIONS && move.toPosition in P2_HOME_POSITIONS) {
                    moveCost += 3.also {
                        logger.v("Add $it by come home")
                    }
                }

                moveCost += ((ai.checkers.count { it.position == from.position } - 1) * 1.3f).toInt()
                    .also {
                        if (it > 0) logger.v("Add by take head:$it")
                    }

                if (!ai.allAtHome && move.toPosition != P2_END) {

                    val notComeHomeCoefficient = if (
                        move.toPosition in P2_HOME_POSITIONS
                        && from.position !in P2_HOME_POSITIONS
                    ) 1 else 2

                    moveCost -= (ai.checkers.count { it.position == move.toPosition } * notComeHomeCoefficient)
                        .also {
                            if (it > 0) logger.v("Decrease by add to head: -$it")
                        }
                }

                if (from.position in P2_HOME_POSITIONS) {
                    moveCost -= (2).also {
                        logger.v("Decrease -$it by move on home")
                    }
                    if (!ai.allAtHome) {
                        moveCost -= (2).also {
                            logger.v("Decrease -$it by move not all at home")
                        }
                    }
                }

                if (ai.allAtHome && move.toPosition == P2_END && move.dices.size == 1) {
                    moveCost += (8).also {
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
        return toPosition < lastPlayerPosition
    }
}
