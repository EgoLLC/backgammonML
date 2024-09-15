package art.vilolon.backgammon.ai

import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.HolePosition

const val AI_MOVE_DELAY = 30L

interface AI {
    suspend fun getMovePosition(game: GGame): HolePosition?
    suspend fun getSelectChecker(game: GGame): GChecker?
}