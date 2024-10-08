package art.vilolon.backgammon.ml

import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.GGameState
import art.vilolon.backgammon.game.rule.P2
import art.vilolon.backgammon.ml.NetworkUtil.NUMBER_OF_INPUTS
import art.vilolon.backgammon.ml.mappers.Mapper
import org.deeplearning4j.rl4j.space.Encodable
import org.nd4j.linalg.api.ndarray.INDArray

class EncodableGame(
    private var game: GGame,
    private val mapper: Mapper,
    private var gameCache: Pair<Int, INDArray>?
) : Encodable {

    @Deprecated("Deprecated in Java")
    override fun toArray(): DoubleArray {
//        return mapper.toInput(game).getMoves().map { it.toDouble() }.toDoubleArray()
        return mapper.toINDArray(game).toDoubleVector()
//        return mapper.toInput(game).getFullBoard().map { it.toDouble() }.toDoubleArray()
            .also {
                println("EncodableGame toArray :${it.joinToString()}")
            }
    }

    override fun isSkipped(): Boolean {
        return game.turnPlayer == P2 || game.state == GGameState.END
    }

    override fun getData(): INDArray {
//        check(game.turnPlayer == P1)

//        val input = mapper.toInput(game)
//        println("EncodableGame getData")
        if (gameCache?.first == game.hashCode()) {
//            println("return cache")
            return gameCache!!.second
        }

//        return Nd4j.create(input.getFullBoard())
        return mapper.toINDArray(game)
//        return Nd4j.create(input.getMoves()) //todo fix input size
            .also { it ->
                gameCache = game.hashCode() to it
//                check(it.size(0) == NUMBER_OF_INPUTS.toLong()) { it.size(0).toString() }
//                it.data().asFloat().mapIndexed { index, fl ->
//                    if (index != 0 && (index + 1) % 24 == 0) {
//                        print(" ${if (fl == 1f) "X" else "_"}\n")
//                    } else {
//                        print(" ${if (fl == 1f) "X" else "_"}")
//                    }
//                    if (index != 0 && (index + 1) % 360 == 0) {
//                        repeat(24) { print(" *") }
//                        println()
//                    }
//                }
//                println("")
            }
    }

    val matrix: INDArray
        get() = mapper.toINDArray(game)
//        get() = Nd4j.create(mapper.toInput(game).getMoves())
            .also {
                println("EncodableGame matrix :${it.size(0)}")
            }

    override fun dup(): Encodable {
        return EncodableGame(game.copy(), mapper, gameCache)
    }

}