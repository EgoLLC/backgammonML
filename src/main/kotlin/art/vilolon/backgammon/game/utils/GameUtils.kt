package art.vilolon.backgammon.game.utils

import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GPlayer
import art.vilolon.backgammon.game.rule.BOARD_HOLE_COUNT
import art.vilolon.backgammon.game.rule.GAME_HOLE_LENGTH
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P1_HEAD
import art.vilolon.backgammon.game.rule.P2
import art.vilolon.backgammon.game.rule.P2_HEAD
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.rule.normalizeP2Position

fun getGameProgress(game: GGame): GameProgress {
    return GameProgress(
        p1Progress = getProgressForPlayer(player = game.player1),
        p2Progress = getProgressForPlayer(player = game.player2),
    )
}

private fun getProgressForPlayer(player: GPlayer): Float {
    val totalPlayerCheckerProgress = player.checkers.map { checker ->
        val turn = if (checker.id >= P_CHECKERS_COUNT) P2 else P1
        val isThrowToEnd = player.allAtHome &&
                (checker.position == P1_HEAD && turn == P1 || checker.position == P2_HEAD && turn == P2)
        when {
            isThrowToEnd -> BOARD_HOLE_COUNT
            turn == P1 -> checker.position - 1
            turn == P2 -> checker.position.normalizeP2Position() - 1
            else -> throw IllegalStateException("Wrong state")
        }
    }.reduce { acc, i ->
        acc + i
    }

    return totalPlayerCheckerProgress.toFloat() / GAME_HOLE_LENGTH.toFloat()
}

data class GameProgress(
    val p1Progress: Float,
    val p2Progress: Float,
)

