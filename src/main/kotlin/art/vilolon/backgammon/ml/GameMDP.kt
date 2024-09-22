package art.vilolon.backgammon.ml

import art.vilolon.backgammon.game.entity.HolePosition
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.P2
import art.vilolon.backgammon.game.utils.GameProgress
import art.vilolon.backgammon.game.utils.Logger
import art.vilolon.backgammon.initNewGym
import art.vilolon.backgammon.ml.NetworkUtil.HIGH_VALUE
import art.vilolon.backgammon.ml.NetworkUtil.LOW_VALUE
import art.vilolon.backgammon.ml.domain.BoardGym
import art.vilolon.backgammon.ml.mappers.Mapper
import kotlinx.coroutines.runBlocking
import org.deeplearning4j.gym.StepReply
import org.deeplearning4j.rl4j.mdp.MDP
import org.deeplearning4j.rl4j.space.DiscreteSpace
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.nd4j.common.primitives.AtomicDouble
import org.nd4j.linalg.api.ndarray.INDArray
import java.math.BigInteger

private const val CHECK_REWARD_STEPS_COUNT = 4_500

class GameMDP(
    private val gameRule: GameRule,
    private val loggerFactory: Logger.Factory,
    private val mapper: Mapper,
    private val maxReward: AtomicDouble,
    private val gym: BoardGym = initNewGym(gameRule, loggerFactory),
) : MDP<EncodableGame, Int, DiscreteSpace> {

    // Size is 360
    private val actionSpace = DiscreteSpace(NetworkUtil.NUMBER_OF_OUTPUTS)
    private var lastProgress: GameProgress = GameProgress(0f, 0f)
    private var bufferReward = 0.0
    private var moveCount = 0L
    private var moveCountCut = 0
    private var startTime: Long = System.currentTimeMillis()
    private var gameCache: Pair<Int, INDArray>? = null

    override fun getObservationSpace(): ObservationSpace<EncodableGame> {
        return GameObservationSpace()
    }

    override fun getActionSpace(): DiscreteSpace {
        return actionSpace
    }

    override fun reset(): EncodableGame {
//        println("Environment reset ${lastProgress}")
        return EncodableGame(gym.newGame(), mapper, gameCache)
    }

    override fun close() {
        val sec = (System.currentTimeMillis() - startTime) / 1000
        println(
            "Close:${moveCount} moves " +
                    "[${(sec / 60 / 60) % 60}:" +
                    "${(sec / 60) % 60}:" +
                    "${sec % 60}]"
        )
    }

    override fun step(actionIndex: Int): StepReply<EncodableGame> {
//        println("Environment step pid:${ProcessHandle.current().pid()}")
        // Find action based on action index
        val (checkerId, toPosition) = mapper.toOutput(actionIndex)
//        println("Environment Index:$actionIndex checkerId:$checkerId to$toPosition")
//        check(checkerId in 0..14 && toPosition in 1..24)
//        check(gym.getTurnPlayer() == P1)
//        check(!isDone)

        var isP2Win: Int? = null
        var isP1Win: Int? = null

        // Get reward
        val rewardValue = calculateRewardForAction(checkerId, toPosition)

        // Move
        runBlocking {
            if (rewardValue == WRONG_MOVE_REWARD) {
                val p1Move = gym.p1Move() //AI2P1 do move
//                p1Move?.winner?.let {
//                        println("P1 Win $p1Move $lastProgress")
//                }
//                isP1Win = p1Move?.winner
//                println("Environment p1Move:$p1Move")
            } else {
                val p1Move = gym.p1Move(checkerId, toPosition)
//                p1Move?.winner?.let {
//                        println("P1 Win $p1Move $lastProgress")
//                }
//                isP1Win = p1Move?.winner //NN do move
//                println("Environment NN p1Move:$p1Move")
            }
        }


        if (gym.getTurnPlayer() == P2) {
            runBlocking {
                while (gym.getTurnPlayer() == P2 && !isDone) {
                    val p2Move = gym.p2Move()
//                    p2Move?.winner?.let {
//                            println("P2 Win $p2Move $lastProgress")
//                    }
//                    isP2Win = p2Move?.winner
//                    println("Environment p2Move:$p2Move")
                }
            }
        }


        lastProgress = gym.getProgress()


        // Get current state
//        check(gym.getTurnPlayer() == P1)
//        encodableGame.game = gym.gameState

        val reward = when {
            isDone -> 0.1
            true == true -> rewardValue
            isP1Win != null -> P1_WIN_REWARD
            isP2Win != null -> rewardValue
            else -> rewardValue
        }

//        println("Environment reward:$reward")

        return StepReply(
            EncodableGame(gym.gameState, mapper, gameCache),
            reward,
            isDone,
            "BackgammonDl4j"
        ).also {
            bufferReward += reward
            moveCount++
            moveCountCut++
            if (moveCountCut == CHECK_REWARD_STEPS_COUNT) {
                val avrReward = bufferReward / CHECK_REWARD_STEPS_COUNT
//                println("Avr reward:${avrReward.toString().take(7)} m:${moveCount}")
                if (maxReward.get() < avrReward) {
                    maxReward.set(avrReward)
                    val sec = (System.currentTimeMillis() - startTime) / 1_000
                    println(
                        "Max reward:${maxReward.get().toString().take(7)} " +
                                "[${(sec / 60 / 60) % 60}:" +
                                "${(sec / 60) % 60}:" +
                                "${sec % 60}]"
//                                + " moveCount ${moveCount}"
                    )
                }
                moveCountCut = 0
                bufferReward = 0.0
            }
        }
    }

    private fun calculateRewardForAction(checkerId: Int, toPosition: HolePosition): Double {
        val p1AvailableMoves = gym.getP1AvailableMoves()
        val availableMovesForChecker = p1AvailableMoves.filter { move ->
            move.holes.any { it.checker.id == checkerId }
        }
        val hasMove = availableMovesForChecker.any { checker ->
            checker.holes.any { it.toPosition == toPosition }
        }

//        println("calculateRewardForAction hasMove:$hasMove \n" +
//                "checker:${availableMovesForChecker.firstOrNull()?.holes?.firstOrNull()?.checker?.position}, \n" +
//                "checkerId:$checkerId \n" +
//                "toPosition:$toPosition \n" )
//                "AvailableMoves: ${p1AvailableMoves.joinToString { "\n$it" }} ")
        if (!hasMove) {
//            println("WRONG_MOVE")
            return WRONG_MOVE_REWARD
        } else {
            return ALLOWED_MOVE_REWARD
        }

        val (p1Progress, p2Progress) = lastProgress

        return when {
            p1Progress >= 1f -> P1_WIN_REWARD
            p2Progress >= 1f -> P2_WIN_REWARD
            p1Progress >= p2Progress -> ALLOWED_MOVE_REWARD + 50.0 // p1 is in the lead
            p1Progress < p2Progress -> ALLOWED_MOVE_REWARD         // p2 is in the lead
            else -> 0.1
        }
    }

    override fun isDone(): Boolean {
        return gym.isGameOver
    }

    override fun newInstance(): MDP<EncodableGame, Int, DiscreteSpace> {
//        gym.newGame()
        return GameMDP(gameRule, loggerFactory, mapper, maxReward)
    }

    companion object {
        const val WRONG_MOVE_REWARD = LOW_VALUE
        private const val ALLOWED_MOVE_REWARD = HIGH_VALUE
        private const val P1_WIN_REWARD = 1000.0
        private const val P2_WIN_REWARD = 10.0
    }
}