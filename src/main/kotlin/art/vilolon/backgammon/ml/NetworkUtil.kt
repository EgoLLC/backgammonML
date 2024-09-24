package art.vilolon.backgammon.ml

import art.vilolon.backgammon.game.rule.BOARD_HOLE_COUNT
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscrete.AsyncNStepQLConfiguration
import org.deeplearning4j.rl4j.learning.configuration.A3CLearningConfiguration
import org.deeplearning4j.rl4j.learning.configuration.AsyncQLearningConfiguration
import org.deeplearning4j.rl4j.network.configuration.ActorCriticDenseNetworkConfiguration
import org.deeplearning4j.rl4j.network.configuration.DQNDenseNetworkConfiguration
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.learning.config.Nadam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File
import kotlin.math.pow

object NetworkUtil {
    const val LOW_VALUE = 0.0
    const val HIGH_VALUE = 1.0
    const val NUMBER_OF_INPUTS = P_CHECKERS_COUNT * BOARD_HOLE_COUNT * 3
    const val NUMBER_OF_OUTPUTS = P_CHECKERS_COUNT * BOARD_HOLE_COUNT

    /*
    *  In  | layer | MAX_STEPS | Time |   %  | CORE  | Arch  |
    * -----|-------|-----------|------|------|-------|-------|
    *  360 |   1   |    92_000 |  1.1 |  4.4 |  CPU  | NStep |
    *  360 |   2   |    92_000 |  1.3 |  3.4 |  CPU  | NStep |
    *  360 |   3   |    92_000 |  2.2 |  2.8 |  CPU  | NStep |
    *  360 |   1   |   920_000 | 12.0 |  8.8 |  CPU  | NStep |
    *  360 |   2   |   920_000 | 26.0 |  7.1 |  CPU  | NStep |
    *  360 |   1   | 4_600_000 |   80 | 11.3 |  CPU  | NStep |
    *  360 |   2   | 4_600_000 |   90 |  8.2 |  CPU  | NStep |
    * -----|-------|-----------|------|------|-------|-------|
    * 1080 |   1   |    92_000 |  2.2 |  0.0 | CPUe5 | NStep |
    * 1080 |   1   |    92_000 |  2.4 |  2.9 |  CPU  |  A3C  |
    * 1080 |   1   |    92_000 |  3.0 |  3.8 |  CPU  | NStep |
    * 1080 |   1   |    92_000 |  3.2 |  4.2 |  M40  | NStep |
    * 1080 |   1   |    92_000 |  4.4 |  3.8 | 750Ti | NStep |
    * -----|-------|-----------|------|------|-------|-------|
    * 1080 |   2   |    92_000 |  3.2 |  0.0 | CPUe5 | NStep |
    * 1080 |   2   |    92_000 |  4.2 |  2.9 |  M40  | NStep |
    * 1080 |   2   |    92_000 |  5.2 |  2.5 |  CPU  | NStep |
    * 1080 |   2   |    92_000 |  7.0 |  2.5 | 750Ti | NStep |
    * -----|-------|-----------|------|------|-------|-------|
    * 1080 |   1   |   920_000 |   23 | 22.0 |  CPU  |  A3C  |
    * 1080 |   1   |   920_000 |   28 |  8.7 |  CPU  | NStep |
    * 1080 |   1   |   920_000 |   32 |  9.9 |  M40  | NStep |
    * 1080 |   1   |   920_000 |   34 |  9.6 | CPUe5 | NStep |
    * 1080 |   1   |   920_000 |   48 | 10.6 | 750Ti | NStep |
    * -----|-------|-----------|------|------|-------|-------|
    * 1080 |   2   |   920_000 |   63 |  3.5 |  CPU  | NStep |
    * -----|-------|-----------|------|------|-------|-------|
    * 1080 |   1   | 4_600_000 |  145 | 12.3 |  M40  | NStep |
    * 1080 |   1   | 4_600_000 |  200 | 11.9 |  CPU  | NStep |
    * 1080 |   1   | 4_600_000 |  282 | 44.2 |  M40  |  A3C  |
    * 1080 |   2   | 4_600_000 |  416 |  6.6 |  CPU  | NStep |
    * */
    private const val STEPS_PER_EPOCH = 460   //460
//    const val MAX_STEPS = 92_000     //200 = 1.1m/4.4% (1080in/3m/3%)
    const val MAX_STEPS = 920_000       //2000 = 12m/8.8% (1080in/27m/10%)
//    const val MAX_STEPS = 4_600_000     //20000 = m / %
    const val RAM_SIZE = 2L * 1024L * 1024L * 1024L
    private const val MAX_THREAD = 6
    private const val LAYERS_COUNT = 1

    // AsyncNStepQLConfiguration.builder()
//    val ASYNC_NSTEP_QL_CONFIGURATION: AsyncQLearningConfiguration = AsyncQLearningConfiguration.builder()
//        .seed(123)
//        .maxEpochStep(STEPS_PER_EPOCH)
//        .maxStep(MAX_STEPS)
//        .numThreads(MAX_THREAD)
//        .nStep(10)
////        .batchSize(32)
////        .doubleDQN(true)
//        .updateStart(100)
//        .rewardFactor(1.0)
//        .gamma(0.999)
//        .errorClamp(1.0)
//        .epsilonNbStep(9_000)
//        .minEpsilon(0.0)
//        .targetDqnUpdateFreq(100)
//        .build()

    var CARTPOLE_A3C: A3CLearningConfiguration = A3CLearningConfiguration.builder()
        .seed(123)
        .maxEpochStep(STEPS_PER_EPOCH)
        .maxStep(MAX_STEPS)
        .numThreads(MAX_THREAD)
        .nStep(10)
        .learnerUpdateFrequency(100)
        .rewardFactor(1.0)
        .gamma(0.999)
        .build()

//    val NET_NSTEP: DQNDenseNetworkConfiguration = DQNDenseNetworkConfiguration
//        .builder()
//        .updater(Adam(0.0001))
//        .numHiddenNodes((NUMBER_OF_OUTPUTS * 1).toInt())
//        .numLayers(LAYERS_COUNT)
////        .l2(0.1)
//        .learningRate(0.1) // 0.001 - 0.1
//        .build()

    val CARTPOLE_NET_A3C: ActorCriticDenseNetworkConfiguration = ActorCriticDenseNetworkConfiguration
        .builder()
        .updater(Adam(0.0001))
        .numHiddenNodes(NUMBER_OF_OUTPUTS)
        .numLayers(LAYERS_COUNT)
//        .l2()
//        .useLSTM()
        .learningRate(0.1)
        .build()

//    fun buildDQNFactory(): DQNFactoryStdDense {
//        val build = DQNDenseNetworkConfiguration.builder()
////            .l2(0.001)
////            .updater(RmsProp(0.000025))
////            .numHiddenNodes(800)
////            .numLayers(5)
////            .build()
//            .updater(Nadam(10.0.pow(-3.5)))
//            .numHiddenNodes(200)
//            .numLayers(6)
//            .build()
//
//        return DQNFactoryStdDense(build)
//    }

    //    var TOY_ASYNC_QL = AsyncNStepQLConfiguration(
//        123,  // Random seed
//        100000,  // Max step By epoch
//        80000,  // Max step
//        8,  // Number of threads
//        5,  // t_max
//        100,  // target update (hard)
//        0,  // num step noop warmup
//        0.1,  // reward scaling
//        0.99,  // gamma
//        10.0,  // td-error clipping
//        0.1f,  // min epsilon
//        2000 // num step for eps greedy anneal
//    )

//    val DQN: QLearningConfiguration = QLearningConfiguration.builder()
//        .seed(123)
//        .maxEpochStep(STEPS_PER_EPOCH)
//        .maxStep(STEPS_PER_EPOCH * MAX_GAMES)
//        .updateStart(0)
//        .rewardFactor(1.0)
//        .gamma(0.999)
//        .errorClamp(1.0)
//        .batchSize(16)
//        .minEpsilon(0.0)
//        .epsilonNbStep(128)
//        .expRepMaxSize(128 * 16)
//        .build()

//    val conf: DQNDenseNetworkConfiguration = DQNDenseNetworkConfiguration.builder()
//        .updater(Nadam(10.0.pow(-3.5)))
//        .numHiddenNodes(360)
//        .numLayers(1)
//        .build()

//    val ASYNC_NSTEP_QL_CONFIGURATION: AsyncNStepQLConfiguration = AsyncNStepQLConfiguration.builder()
//        .seed(123)
//        .maxEpochStep(STEPS_PER_EPOCH)
//        .maxStep(STEPS_PER_EPOCH * MAX_GAMES)
//        .numThread(MAX_THREAD)
//        .nstep(10)
//        .updateStart(0)
//        .rewardFactor(1.0)
//        .gamma(0.99)
//        .errorClamp(1.0)
//        .epsilonNbStep(90)
//        .minEpsilon(0.0f)
//        .targetDqnUpdateFreq(100)
//        .build()
//
//    val NET_NSTEP: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
//        .updater(Adam(0.001))
//        .numHiddenNodes(360)
//        .numLayer(1)
////        .l2(0.01)
//        .build()

//    val A3C: A3CConfiguration = A3CConfiguration
//        .builder()
//        .seed(123)
//        .maxEpochStep(STEPS_PER_EPOCH)
//        .maxStep(STEPS_PER_EPOCH * MAX_GAMES)
//        .numThread(MAX_THREAD)
//        .nstep(10)
//        .updateStart(0)
//        .rewardFactor(1.0)
//        .gamma(0.99)
//        .errorClamp(1.0)
//        .build()

//    var configuration: ActorCriticFactorySeparateStdDense.Configuration =
//        ActorCriticFactorySeparateStdDense.Configuration
//            .builder()
//            .updater(Adam(0.001))
//            .l2(0.1)
//            .numHiddenNodes(360)
//            .numLayer(3)
//            .build()


//    val multiLayerConfiguration: MultiLayerConfiguration = NeuralNetConfiguration.Builder()
//        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//        .updater(Nesterovs(learningRate, 0.9))
//        .list(
//            DenseLayer.Builder()
//                .nIn(numInputs)
//                .nOut(numHiddenNodes)
//                .activation(Activation.RELU)
//                .build(),
//            OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
//                .activation(Activation.SOFTMAX)
//                .nIn(numHiddenNodes)
//                .nOut(numOutputs)
//                .build()
//        )
//        .backpropType(BackpropType.Standard)
//        .build()

    private fun multiLayerConfiguration() {
        val numInputs = NUMBER_OF_INPUTS
        val numOutputs = NUMBER_OF_OUTPUTS
        val conf = NeuralNetConfiguration.Builder()
            .seed(123) //include a random seed for reproducibility
            // use stochastic gradient descent as an optimization algorithm
            .updater(Nadam()) //specify the rate of change of the learning rate.
            .l2(1e-4)
            .list()
            .layer(
                DenseLayer.Builder() //create the first, input layer with xavier initialization
                    .nIn(numInputs)
                    .nOut(1000)
                    .activation(Activation.RELU)
                    .weightInit(WeightInit.XAVIER)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                    .nIn(1000)
                    .nOut(numOutputs)
                    .activation(Activation.SOFTMAX)
                    .weightInit(WeightInit.XAVIER)
                    .build()
            )
            .build()

        val model = MultiLayerNetwork(conf)

//        model.
    }

//    fun create(inputCoder: InputCoder, outputCoder: OutputCoder, hidden: Int): BackgammonNeuralNetwork? {
//        //https://github.com/dabisa/Backgammon/blob/master/bglib/src/main/java/com/dkelava/backgammon/bglib/nn/BackgammonNeuralNetwork.java
//        val seed = 0
//        val alpha = 0.1
//        val nInputs: Int = inputCoder.getInputSize()
//        val nOutput: Int = outputCoder.getSize()
//
//        val conf: MultiLayerConfiguration = NeuralNetConfiguration.Builder()
//            .seed(seed.toLong())
//            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
////            .iterations(1)
////            .learningRate(alpha)
//            .updater(Updater.NONE)
//            .regularization(emptyList())
//            .list()
//            .layer(
//                0,
//                DenseLayer.Builder()
//                    .nIn(nInputs)
//                    .nOut(hidden)
//                    .activation(Activation.SIGMOID)
//                    .weightInit(WeightInit.UNIFORM)
//                    .build()
//            )
//            .layer(
//                1,
//                OutputLayer.Builder(LossFunctions.LossFunction.MSE)
//                    .nIn(hidden)
//                    .nOut(nOutput)
//                    .activation(Activation.SIGMOID)
//                    .weightInit(WeightInit.UNIFORM)
//                    .build()
//            )
////            .pretrain(false)
////            .backprop(true)
//            .build()
//
//        val model = MultiLayerNetwork(conf)
//        model.init()
//
//        return BackgammonNeuralNetwork(model, inputCoder, outputCoder)
//    }

    fun buildConfig(): AsyncNStepQLConfiguration {


//        AsyncNStepQLearningDiscrete.AsyncNStepQLConfiguration
//        NeuralNetConfiguration.Builder()
//            .seed(123L)
//            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
//            .iterations(1)
//            .learningRate(0.1)
//            .rmsDecay(0.95)
//            .seed(12345)
//            .regularization(true)
//            .l2(0.01)
//            .weightInit(WeightInit.XAVIER)
//            .updater(Nesterovs(0.1))
//            .list()
//            .layer(
//                0,
//                GravesLSTM.Builder().nIn(iter.inputColumns())
//                    .nOut(lstmLayerSize)
//                    .activation(Activation.RELU6).build()
//            )
//            .layer(
//                1,
//                RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
//                    .activation(Activation.SOFTMAX)
//                    .nIn(lstmLayerSize).nOut(nOut).build()
//            )
//            .backpropType(BackpropType.TruncatedBPTT)
//            .tBPTTForwardLength(tbpttLength)
//            .tBPTTBackwardLength(tbpttLength)
//            .pretrain(true)
//            .backprop(true)
//            .build()

        return AsyncNStepQLConfiguration.builder()
            .seed(123)
            .maxEpochStep(STEPS_PER_EPOCH)
            .maxStep(STEPS_PER_EPOCH * MAX_STEPS)
            .numThread(4)
            .nstep(10)
            .updateStart(0)
            .rewardFactor(0.1)
            .gamma(0.99)
            .errorClamp(1.0)
            .epsilonNbStep(9000)
            .minEpsilon(0f)
            .targetDqnUpdateFreq(100)
            .build()

        //snake example
//        return QLearningConfiguration.builder()
//            .seed(123L)
//            .maxEpochStep(200)
//            .maxStep(15_000)
//            .expRepMaxSize(150_000)
//            .batchSize(128)
//            .targetDqnUpdateFreq(500)
//            .updateStart(10)
//            .rewardFactor(0.01)
//            .gamma(0.99)
//            .errorClamp(1.0)
//            .minEpsilon(0.1)
//            .epsilonNbStep(1000)
//            .doubleDQN(true)
//            .build()

//        return QLearningConfiguration.builder()
//            .seed(1L)
//            .maxEpochStep(1000)
//            .maxStep(1000 * 50)
//            .updateStart(0)
//            .rewardFactor(1.0)
//            .gamma(0.999)
//            .errorClamp(1.0)
//            .batchSize(16)
//            .minEpsilon(0.0)
//            .epsilonNbStep(128)
//            .expRepMaxSize(128 * 16)
//            .build()

    }

//    val netConf: DQNFactoryStdDense.Configuration = DQNFactoryStdDense.Configuration.builder()
//        .l2(0.001)
//        .updater(Adam(0.0005))
//        .numHiddenNodes(16)
//        .numLayer(3)
//        .build()


    fun loadNetwork(networkName: String): MultiLayerNetwork {
        return MultiLayerNetwork.load(File(networkName), true)
    }

}