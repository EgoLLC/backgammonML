package art.vilolon.backgammon.ml

import art.vilolon.backgammon.game.rule.BOARD_HOLE_COUNT
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.ml.NetworkUtil.NUMBER_OF_INPUTS
import org.deeplearning4j.rl4j.mdp.CartpoleNative
import org.deeplearning4j.rl4j.space.ObservationSpace
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

class GameObservationSpace : ObservationSpace<EncodableGame> {

    override fun getName(): String {
        return "BackgammonGameObservationSpace"
    }

    override fun getShape(): IntArray {
//        return IntArray(1) { NUMBER_OF_INPUTS }
        return intArrayOf(NUMBER_OF_INPUTS)
    }

    override fun getLow(): INDArray {
        return Nd4j.create(LOWS)
    }

    override fun getHigh(): INDArray {
        return Nd4j.create(HIGHS)
    }

    companion object {
        private val LOWS = createValueArray(NetworkUtil.LOW_VALUE)
        private val HIGHS = createValueArray(NetworkUtil.HIGH_VALUE)

        private fun createValueArray(value: Double): DoubleArray {
//            println("GameObservationSpace createValueArray")
            return DoubleArray(NUMBER_OF_INPUTS) { value }
//            val values = DoubleArray(NUMBER_OF_INPUTS)
//            for (i in 0 until NUMBER_OF_INPUTS) {
//                values[i] = value
//            }

//            return values
        }
    }
}