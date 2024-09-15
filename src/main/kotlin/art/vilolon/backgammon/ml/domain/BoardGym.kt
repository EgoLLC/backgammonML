package art.vilolon.backgammon.ml.domain

import art.vilolon.backgammon.ai.AI
import art.vilolon.backgammon.game.entity.GAvailableMoves
import art.vilolon.backgammon.game.entity.GDice
import art.vilolon.backgammon.game.entity.GDices
import art.vilolon.backgammon.game.entity.GDicesOnBoard
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GGameState
import art.vilolon.backgammon.game.entity.HolePosition
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.NEW_GAME_P2AI
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P1_HEAD
import art.vilolon.backgammon.game.rule.P1_HOME_POSITIONS
import art.vilolon.backgammon.game.rule.P2
import art.vilolon.backgammon.game.rule.P2_HEAD
import art.vilolon.backgammon.game.rule.P2_HOME_POSITIONS
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.utils.getGameProgress
import art.vilolon.backgammon.ml.model.Output
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
    throwable.printStackTrace()
}
val scope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)

//fun main() = runBlocking {

//    val jobCount = 1
//    var t = 2000
//    val p1Win = AtomicInteger()
//    val p2Win = AtomicInteger()
//    val measureTimeMillis = measureTimeMillis {
//        buildList {
//            coroutineScope {
//                val loggerFactory = S4jLoggerFactory()
//                val gameRule = GameRule(loggerFactory)
//                repeat(jobCount) {
//                    add(
//                        scope.launch {
//                            val boardGym = BoardGym(
//                                boardId = it,
//                                totalBoard = jobCount,
//                                gameRule = gameRule,
//                                p1 = AI2(gameRule, loggerFactory), // set lvl 3
//                                p2 = AI2P2(gameRule, loggerFactory),
//                                gameState = NEW_GAME_P2AI.copy(
//                                    dices = GDicesOnBoard(
//                                        leftBoard = GDices(gameRule.rollingDices(2)),
//                                    )
//                                ),
//                            )
//                            while (t-- > 0) {
//                                if (boardGym.getTurnPlayer() == P2) {
//                                    boardGym.p2Move()
//                                } else {
//                                    boardGym.p1Move(null, null)
//                                }
//                            }
////                            val winner = boardGym.startGame()
////                            if (winner == 1) p1Win.incrementAndGet() else p2Win.incrementAndGet()
//                        }
//                    )
//                }
//            }
//        }.joinAll()
//    }
//
//    println("Win p1:$p1Win p2:$p2Win | time:$measureTimeMillis")
//}

class WrongMove(checkerId: Int?, toPosition: HolePosition?) : IllegalStateException("Wrong move $checkerId, $toPosition")

class BoardGym(
    private val gameRule: GameRule,
    private val p1: AI,
    private val p2: AI,
    private val boardId: Int = 0,
    private val totalBoard: Int = 0,
    var gameState: GGame,
) {

    private val firstMove = gameState.turnPlayer
    private var moveCount = 0
    private var modelDoMoveCount = 0

    fun getTurnPlayer() = gameState.turnPlayer

    fun getProgress() = getGameProgress(gameState)

    fun getP1AvailableMoves(): List<GAvailableMoves> = with(gameState) {
        gameRule.getMovesAndCheckersForPlayer(
            player = player1,
            opponent = player2,
            allDices = dices.values,
            turnPlayer = P1
        ).values.map { GAvailableMoves(holes = it) }.toList()
    }

    val isGameOver: Boolean get() = gameState.state == GGameState.END

    fun newGame(): GGame {
        gameState = NEW_GAME_P2AI.copy(
            turnPlayer = P1,
            dices = GDicesOnBoard(
                leftBoard = GDices(
                    values = buildList {
                        val newDices = gameRule.rollingDices(2)
                        if (newDices[0].value == newDices[1].value) {
                            repeat(4) {
                                add(
                                    GDice(value = newDices.first().value)
                                )
                            }
                        } else {
                            add(newDices[0])
                            add(newDices[1])
                        }
                    }
                ),
            )
        )
        return gameState
    }

    suspend fun p2Move(): Output? {
//        println("p2Move")
//        println("p2Move ${getProgress()}")
        check(gameState.turnPlayer == P2 && gameState.state == GGameState.PLAYING) {
            gameState
        }

        val checker = p2.getSelectChecker(gameState)
        if (checker == null) {
            noMoveNextMove(P1)
            return null
        }

        val availableMoves = gameRule.getAvailableMoves(
            gameState.player2,
            gameState.player1,
            gameState.dices.leftBoard.values,
            gameState.turnPlayer,
            checker,
        )
        gameState = gameState.copy(availableMoves = availableMoves)

        val moveTo = p2.getMovePosition(gameState)
        if (moveTo == null) {
            noMoveNextMove(P1)
            return null
        }

        val move = availableMoves.holes.find {
            it.toPosition == moveTo && it.checker.position == checker.position // Get any checker on hole
        }
        checkNotNull(move)

        val updatedDice = gameState.dices.leftBoard.values.map { dice ->
            if (move.dices.any { dice.id == it.id }) {
                dice.copy(isUsed = true)
            } else {
                dice
            }
        }
        val isMoveFinish = updatedDice.all { it.isUsed }
        if (isMoveFinish && gameState.turnPlayer != firstMove) {
            moveCount++
        }

        val tookHead = if (isMoveFinish) false else gameState.player2.tookHead || move.checker.position == P2_HEAD
        val updateP2 = gameState.player2.copy(
            checkers = gameState.player2.checkers.map { p2Checker ->
                if (move.checker == p2Checker) p2Checker.copy(position = move.toPosition) else p2Checker
            },
            tookHead = tookHead,
            allAtHome = gameState.player2.allAtHome || (
                    (gameState.player2.checkers.count { it.position in P2_HOME_POSITIONS } == P_CHECKERS_COUNT - 1)
                            && (move.checker.position !in P2_HOME_POSITIONS && move.toPosition in P2_HOME_POSITIONS)
                    )
        )

        val isGameOver = moveCount > 10 && updateP2.checkers.all { it.position == P2_HEAD }

        var moveCountOutput: Int = 0
        var modelDoMoveCountOutput: Int = 0

        gameState = if (isGameOver) {
//            println("    p1 ${gameState.player1.checkers.joinToString { it.position.toString() }}")
//            println("win p2 ${gameState.player2.checkers.joinToString { it.position.toString() }}")
            moveCountOutput = moveCount
            modelDoMoveCountOutput = modelDoMoveCount
            moveCount = 0
            modelDoMoveCount = 0
            NEW_GAME_P2AI.copy(
                turnPlayer = P1,
                dices = GDicesOnBoard(
                    leftBoard = GDices(values = if (isMoveFinish) getNewDices() else updatedDice)
                ),
                player2 = updateP2,
                state = GGameState.END,
            )
        } else {
            gameState.copy(
                turnPlayer = if (isMoveFinish) P1 else P2,
                player2 = updateP2,
                dices = GDicesOnBoard(
                    leftBoard = GDices(values = if (isMoveFinish) getNewDices() else updatedDice)
                ),
            )
        }

        return Output(
            checkerId = checker.id,
            to = moveTo,
            winner = if (isGameOver) P2 else null,
            moves = moveCountOutput,
            modelDoMoves = modelDoMoveCountOutput
        )
    }

    @Throws(WrongMove::class)
    suspend fun p1Move(checkerId: Int? = null, toPosition: HolePosition? = null): Output? {
//        println("p1Move ${getProgress()}")
//        val line1 = ("p1Move checkerId:${checkerId}, toPosition:$toPosition\n")
//        check(gameState.turnPlayer == P1 && gameState.state == GGameState.PLAYING)

        val moveCheckerPosition = checkerId?.let { gameState.player1.checkers.find { it.id == checkerId }?.position }

        val topChecker = moveCheckerPosition?.let {
            gameState.player1.checkers.maxByOrNull { checker ->
                moveCheckerPosition == checker.position
            }
        }
//        val line2 = ("p1Move topChecker:${topChecker}\n")

        val checker = topChecker ?: p1.getSelectChecker(gameState)
        if (checker == null) {
            noMoveNextMove(P2)
            return null
        }

//        val line3 = ("p1Move select checker:${checker}\n")

        val availableMoves = gameRule.getAvailableMoves(
            gameState.player1,
            gameState.player2,
            gameState.dices.leftBoard.values,
            gameState.turnPlayer,
            checker,
        )
        gameState = gameState.copy(availableMoves = availableMoves)

//        val line4 = ("p1Move availableMoves for checker:${gameState.availableMoves.holes.joinToString { "\n$it" }}")

        val moveTo = toPosition ?: p1.getMovePosition(gameState)
        if (moveTo == null) {
            noMoveNextMove(P2)
            return null
        }

        val move = availableMoves.holes.find {
            it.toPosition == moveTo && it.checker.position == checker.position // Get any checker on hole
        }
//        check(move != null) {
//            val allAvailableMoves = availableMoves.holes.joinToString()
//            line1 + line2 + line3 + line4 + allAvailableMoves
//        }

        if (toPosition != null && topChecker != null && toPosition != null && move != null) {
            modelDoMoveCount++
        }

        val updatedDice = gameState.dices.leftBoard.values.map { dice ->
            if (move!!.dices.any { dice.id == it.id }) {
                dice.copy(isUsed = true)
            } else {
                dice
            }
        }
        val isMoveFinish = updatedDice.all { it.isUsed }
        if (isMoveFinish && gameState.turnPlayer != firstMove) {
            moveCount++
        }

        val tookHead = if (isMoveFinish) false else gameState.player1.tookHead || move!!.checker.position == P1_HEAD
        val updateP1 = gameState.player1.copy(
            checkers = gameState.player1.checkers.map { p1Checker ->
                if (move!!.checker == p1Checker) p1Checker.copy(position = move.toPosition) else p1Checker
            },
            tookHead = tookHead,
            allAtHome = gameState.player1.allAtHome || (
                    (gameState.player1.checkers.count { it.position in P1_HOME_POSITIONS } == P_CHECKERS_COUNT - 1)
                            && (move!!.checker.position !in P1_HOME_POSITIONS && move.toPosition in P1_HOME_POSITIONS)
                    )
        )

        val isGameOver = moveCount > 10 && updateP1.checkers.all { it.position == P1_HEAD }

        var moveCountOutput = 0
        var modelDoMoveCountOutput = 0

        gameState = if (isGameOver) {
//            println("win p1 ${gameState.player1.checkers.joinToString { it.position.toString() }}")
//            println("    p2 ${gameState.player2.checkers.joinToString { it.position.toString() }}")
            moveCountOutput = moveCount
            modelDoMoveCountOutput = modelDoMoveCount
            moveCount = 0
            modelDoMoveCount = 0
            NEW_GAME_P2AI.copy(
                turnPlayer = P1,
                dices = GDicesOnBoard(
                    leftBoard = GDices(values = if (isMoveFinish) getNewDices() else updatedDice)
                ),
                player1 = updateP1,
                state = GGameState.END,
            )
        } else {
            gameState.copy(
                turnPlayer = if (isMoveFinish) P2 else P1,
                player1 = updateP1,
                dices = GDicesOnBoard(leftBoard = GDices(values = if (isMoveFinish) getNewDices() else updatedDice)),
            )
        }

        return Output(
            checkerId = checker.id,
            to = moveTo,
            winner = if (isGameOver) P1 else null,
            moves = moveCountOutput,
            modelDoMoves = modelDoMoveCountOutput,
        )
    }

    private fun getNewDices(): List<GDice> {
//        println("updatedDice")
        var newDices = gameRule.rollingDices(2)
        if (newDices[0].value == newDices[1].value) {
            newDices = buildList {
                repeat(4) {
                    add(
                        GDice(value = newDices.first().value)
                    )
                }
            }
        }
        return newDices
    }

    private fun noMoveNextMove(nextPlayer: Int) {
        gameState = gameState.copy(
            turnPlayer = nextPlayer,
            player1 = gameState.player1.copy(tookHead = false),
            player2 = gameState.player2.copy(tookHead = false),
            dices = GDicesOnBoard(leftBoard = GDices(values = getNewDices()))
        )
    }

//    suspend fun startGame(): Int {
//
//        while (gameState.state == GGameState.PLAYING) {
//
////            println("P${gameState.turnPlayer} moveCount:$moveCount\n" +
////                    "dices=${gameState.dices.values.filter { !it.isUsed }.joinToString { it.value.toString() }}\n" +
////                    "p1=${gameState.player1.checkers.joinToString { it.position.toString() }}\n" +
////                    "p2=${gameState.player2.checkers.joinToString { it.position.toString() }}\n"
////            )
//
//            val isTurnP1 = gameState.turnPlayer == P1
//
//            val checker = if (isTurnP1) p1.getSelectChecker(gameState) else p2.getSelectChecker(gameState)
//
//            val nextPlayer = if (isTurnP1) P2 else P1
//            if (checker == null) {
//                gameState = gameState.copy(
//                    turnPlayer = nextPlayer,
//                    player1 = gameState.player1.copy(tookHead = false),
//                    player2 = gameState.player2.copy(tookHead = false),
//                    dices = GDicesOnBoard(
//                        leftBoard = GDices(
//                            values = buildList {
//                                val newDices = gameRule.rollingDices(2)
//                                if (newDices[0].value == newDices[1].value) {
//                                    repeat(4) {
//                                        add(
//                                            GDice(value = newDices.first().value)
//                                        )
//                                    }
//                                } else {
//                                    add(newDices[0])
//                                    add(newDices[1])
//                                }
//                            }
//                        )
//                    )
//                )
//                continue
//            }
//
//            val availableMoves = gameRule.getAvailableMoves(
//                if (isTurnP1) gameState.player1 else gameState.player2,
//                if (isTurnP1) gameState.player2 else gameState.player1,
//                gameState.dices.leftBoard.values,
//                gameState.turnPlayer,
//                checker,
//            )
//
//            gameState = gameState.copy(availableMoves = availableMoves)
//
//            val moveTo = if (isTurnP1) p1.getMovePosition(gameState) else p2.getMovePosition(gameState)
//
//            if (moveTo == null) {
//                gameState = gameState.copy(
//                    turnPlayer = nextPlayer,
//                    player1 = gameState.player1.copy(tookHead = false),
//                    player2 = gameState.player2.copy(tookHead = false),
//                    dices = GDicesOnBoard(
//                        leftBoard = GDices(
//                            values = buildList {
//                                val newDices = gameRule.rollingDices(2)
//                                if (newDices[0].value == newDices[1].value) {
//                                    repeat(4) {
//                                        add(
//                                            GDice(value = newDices.first().value)
//                                        )
//                                    }
//                                } else {
//                                    add(newDices[0])
//                                    add(newDices[1])
//                                }
//                            }
//                        )
//                    )
//                )
//                continue
//            }
//
//            val move = availableMoves.holes.find { it.toPosition == moveTo && it.checker.position == checker.position }
//            checkNotNull(move)
//
//            val updatedDice = gameState.dices.leftBoard.values.map { dice ->
//                if (move.dices.any { dice.id == it.id }) {
//                    dice.copy(isUsed = true)
//                } else {
//                    dice
//                }
//            }
//            val isMoveFinish = updatedDice.all { it.isUsed }
//            if (isMoveFinish && gameState.turnPlayer == firstMove) {
//                moveCount++
//            }
//
//            val updateP1 = if (isTurnP1) {
//                val tookHead = if (isMoveFinish) false else gameState.player1.tookHead || move.checker.position == P1_HEAD
//                gameState.player1.copy(
//                    checkers = gameState.player1.checkers.map { p1Checker ->
//                        if (move.checker == p1Checker) p1Checker.copy(position = move.toPosition) else p1Checker
//                    },
//                    tookHead = tookHead,
//                    allAtHome = gameState.player1.allAtHome || (
//                            (gameState.player1.checkers.count { it.position in P1_HOME_POSITIONS } == P_CHECKERS_COUNT - 1)
//                                    && (move.checker.position !in P1_HOME_POSITIONS && move.toPosition in P1_HOME_POSITIONS)
//                            )
//                )
//            } else {
//                gameState.player1
//            }
//
//            val updateP2 = if (!isTurnP1) {
//                val tookHead = if (isMoveFinish) false else gameState.player2.tookHead || move.checker.position == P2_HEAD
//                gameState.player2.copy(
//                    checkers = gameState.player2.checkers.map { p2Checker ->
//                        if (move.checker == p2Checker) p2Checker.copy(position = move.toPosition) else p2Checker
//                    },
//                    tookHead = tookHead,
//                    allAtHome = gameState.player2.allAtHome || (
//                            (gameState.player2.checkers.count { it.position in P2_HOME_POSITIONS } == P_CHECKERS_COUNT - 1)
//                                    && (move.checker.position !in P2_HOME_POSITIONS && move.toPosition in P2_HOME_POSITIONS)
//                            )
//                )
//            } else {
//                gameState.player2
//            }
//
//            val isGameOver = moveCount > 2 && (updateP1.checkers.all { it.position == P1_HEAD }
//                    || updateP2.checkers.all { it.position == P2_HEAD })
//
//            gameState = gameState.copy(
//                turnPlayer = if (isMoveFinish) nextPlayer else gameState.turnPlayer,
//                player1 = updateP1,
//                player2 = updateP2,
//                state = if (!isGameOver) GGameState.PLAYING else GGameState.END,
//                dices = GDicesOnBoard(
//                    leftBoard = GDices(
//                        values = if (isMoveFinish) {
//                            var newDices = gameRule.rollingDices(2)
//                            if (newDices[0].value == newDices[1].value) {
//                                newDices = buildList {
//                                    repeat(4) {
//                                        add(
//                                            GDice(value = newDices.first().value)
//                                        )
//                                    }
//                                }
//                            }
//                            newDices
//                        } else updatedDice,
//                    )
//                ),
//            )
//
//            if (isGameOver) {
////                println("$boardId Game over, P${if (gameState.player1.checkers.all { it.position == P1_HEAD }) "1" else "2"} win")
//            }
//        }
//        return if (gameState.player1.checkers.all { it.position == P1_HEAD }) P1 else P2
//    }

}