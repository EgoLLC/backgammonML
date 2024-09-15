package art.vilolon.backgammon

//import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.DQNFactoryStdDense;
//import org.deeplearning4j.rl4j.agent.learning.algorithm.dqn.IDQN;
import art.vilolon.backgammon.ai.AI2
import art.vilolon.backgammon.ai.AI2P2
import art.vilolon.backgammon.game.entity.GDice
import art.vilolon.backgammon.game.entity.GDices
import art.vilolon.backgammon.game.entity.GDicesOnBoard
import art.vilolon.backgammon.game.entity.GGameState
import art.vilolon.backgammon.game.rule.GameRule
import art.vilolon.backgammon.game.rule.NEW_GAME_P2AI
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P_CHECKERS_COUNT
import art.vilolon.backgammon.game.utils.LOGGER_FACTORY
import art.vilolon.backgammon.game.utils.Logger
import art.vilolon.backgammon.ml.EncodableGame
import art.vilolon.backgammon.ml.GameMDP
import art.vilolon.backgammon.ml.GameMDP.Companion.WRONG_MOVE_REWARD
import art.vilolon.backgammon.ml.NetworkUtil
import art.vilolon.backgammon.ml.NetworkUtil.ASYNC_NSTEP_QL_CONFIGURATION
import art.vilolon.backgammon.ml.NetworkUtil.NET_NSTEP
import art.vilolon.backgammon.ml.domain.BoardGym
import art.vilolon.backgammon.ml.mappers.Mapper
import org.datavec.api.records.reader.impl.regex.RegexSequenceRecordReader.LOG
import org.deeplearning4j.core.storage.StatsStorage
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.optimize.listeners.PerformanceListener
import org.deeplearning4j.rl4j.learning.IEpochTrainer
import org.deeplearning4j.rl4j.learning.ILearning
import org.deeplearning4j.rl4j.learning.async.nstep.discrete.AsyncNStepQLearningDiscreteDense
import org.deeplearning4j.rl4j.learning.listener.TrainingListener
import org.deeplearning4j.rl4j.space.ActionSpace
import org.deeplearning4j.rl4j.util.DataManager
import org.deeplearning4j.rl4j.util.DataManagerTrainingListener
import org.deeplearning4j.rl4j.util.IDataManager
import org.nd4j.common.primitives.AtomicDouble
//import org.nd4j.jita.conf.CudaEnvironment
import org.nd4j.linalg.api.ndarray.INDArray
import java.io.IOException
import java.util.*

fun main() {
    start()
}

val maxReward = AtomicDouble(WRONG_MOVE_REWARD)

fun start() {
    val randomNetworkName = "network-" + System.currentTimeMillis() + ".zip"
    val calendar = Calendar.getInstance()
    val h = calendar.get(Calendar.HOUR_OF_DAY)
    val m = calendar.get(Calendar.MINUTE)
    val s = calendar.get(Calendar.SECOND)
    println("Start NetworkName:$randomNetworkName pid:${ProcessHandle.current().pid()} $h:$m:$s")
    val mapper = Mapper()
    val gameRule = GameRule(LOGGER_FACTORY)

    System.setProperty("org.bytedeco.openblas.load", "mkl");
    initCUDA()

    // Create our training environment
    val mdp = GameMDP(gameRule, LOGGER_FACTORY, mapper, maxReward)
//    val dql: QLearningDiscreteDense<EncodableGame> = QLearningDiscreteDense(
//        mdp,
//        NetworkUtil.buildDQNFactory(),
//        NetworkUtil.buildConfig(),
//    )

//    val mdp: MDP<Game, Int, DiscreteSpace> = SnakeGameMdp(width, height, Random(7))

    val dql: AsyncNStepQLearningDiscreteDense<EncodableGame> = AsyncNStepQLearningDiscreteDense(
        mdp,
        NET_NSTEP,
        ASYNC_NSTEP_QL_CONFIGURATION,
    )

//    dql.addListener(DataManagerTrainingListener(DataManager()))

//    initVisualisation(dql)
//    dql.addListener(trainingListener)
    val trainingListener = object : TrainingListener {
        override fun onTrainingStart(): TrainingListener.ListenerResponse {
            println("Training Start")
            return TrainingListener.ListenerResponse.CONTINUE
        }

        override fun onTrainingEnd() {
            println("Training End")
        }

        override fun onNewEpoch(trainer: IEpochTrainer?): TrainingListener.ListenerResponse {
            println("New Epoch: ${trainer?.mdp?.observationSpace?.shape?.joinToString()}")
            return TrainingListener.ListenerResponse.CONTINUE
        }

        override fun onEpochTrainingResult(
            trainer: IEpochTrainer?,
            statEntry: IDataManager.StatEntry?,
        ): TrainingListener.ListenerResponse {
            println("Epoch Training Result: ${statEntry?.reward} ${trainer?.mdp?.observationSpace?.shape?.joinToString()}")
            return TrainingListener.ListenerResponse.CONTINUE
        }

        override fun onTrainingProgress(learning: ILearning<*, *, out ActionSpace<*>>?): TrainingListener.ListenerResponse {
//            println("Epoch Training Progress: ${learning?.mdp?.observationSpace?.shape?.joinToString()}")
            return TrainingListener.ListenerResponse.CONTINUE
        }
    }

//    val dql: A3CDiscreteDense<EncodableGame> = A3CDiscreteDense(mdp, CARTPOLE_NET_A3C, CARTPOLE_A3C)

//    val dql: A3CDiscreteDense<EncodableGame> = A3CDiscreteDense(mdp, configuration, A3C)
//    val dql: QLearningDiscreteDense<EncodableGame> = QLearningDiscreteDense(mdp, conf, DQN)

//    val  dqn = DQNFactoryStdDense.Configuration.builder()
//        .build()
//        .buildDQN(netConf)
//    val policy: DQNPolicy  = DQNPolicy<>(dqn)


    /*
public static void main(String[] args) {
   // Создание среды и агента
   MDP mdp = ...; // Определите вашу среду
   DQNFactoryStdDense.Configuration netConf = DQNFactoryStdDense.Configuration.builder()
       .l2(0.001)
       .updater(new Adam(0.0005))
       .numHiddenNodes(16)
       .numLayer(3)
       .build();
   IDQN dqn = DQNFactoryStdDense.buildDQN(netConf);
   DQNPolicy policy = new DQNPolicy<>(dqn);
   DQNTrainer trainer = new DQNTrainer<>(mdp, policy, 10000);

   // Обучение агента
   trainer.train();

   // Использование обученной политики
   Environment environment = ...; // Определите вашу среду
   while (!environment.isDone()) {
       StepReply stepReply = environment.step(policy.nextAction(environment.getObservation()));
       System.out.println("Reward: " + stepReply.getReward());
   }
}
*/


    // Start the training
    println("Start train pid:${ProcessHandle.current().pid()}")
    dql.train()
    mdp.close()


    // Save network
    try {
        dql.neuralNet.save(randomNetworkName)
//        dql.neuralNet.neuralNetworks[0].
        println("saved:$randomNetworkName pid:${ProcessHandle.current().pid()}")
    } catch (e: IOException) {
        LOG.error(e.message, e)
    }

    // Reset the game
//    gym.newGame()

    // Evaluate just trained network
//    evaluateNetwork(gym, randomNetworkName, mapper, gameRule, loggerFactory)
}

fun initCUDA() {
//    CudaEnvironment.getInstance().configuration
//        .allowMultiGPU(true) // key option enabled
//        .setMaximumDeviceCache(2L * 1024L * 1024L * 1024L) // we're allowing larger memory caches
//        .allowCrossDeviceAccess(true) // cross-device access is used for faster model averaging over pcie
}

fun initVisualisation(dql: AsyncNStepQLearningDiscreteDense<EncodableGame>) {
    //Initialize the user interface backend
    //Initialize the user interface backend
//    val uiServer: UIServer = UIServer.getInstance()

    //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.

    //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
//    val statsStorage: StatsStorage = InMemoryStatsStorage() //Alternative: new FileStatsStorage(File), for saving and loading later

    //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
//    uiServer.attach(statsStorage)
//    val dataManager = DataManager()
//    uiServer.attach(dataManager)


    //Then add the StatsListener to collect this information from the network, as it trains

//        DataManagerTrainingListener(dataManager)
//    )
}

fun initNewGym(
    gameRule: GameRule,
    loggerFactory: Logger.Factory,
) = BoardGym(
    gameRule = gameRule,
    p1 = AI2(gameRule, loggerFactory), //AI2(gameRule, loggerFactory), // Set lvl 3
    p2 = AI2P2(gameRule, loggerFactory),
    gameState = NEW_GAME_P2AI.copy(
        state = GGameState.PLAYING,
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
)


private fun evaluateNetwork(
    gym: BoardGym,
    randomNetworkName: String,
    mapper: Mapper,
    gameRule: GameRule,
    loggerFactory: Logger.Factory,
) {
    val multiLayerNetwork: MultiLayerNetwork = NetworkUtil.loadNetwork(randomNetworkName)
    var highscore = 0

    var score = 0
    repeat(99) {
        try {
            val state = EncodableGame(gym.gameState, mapper)
            val output: INDArray = multiLayerNetwork.output(state.matrix, false)
            val data = output.data().asFloat()

//            val maxValueIndex: Int = GameStateUtil.getMaxValueIndex(data)

//            game.changeDirection(Action.getActionByIndex(maxValueIndex))
//            game.move()
//            score = game.getScore()

            // Needed so that we can see easier what is the game doing
//                NetworkUtil.waitMs(0)
        } catch (e: Exception) {
            LOG.error(e.message, e)
            Thread.currentThread().interrupt()
//                game.endGame()
        }
    }

    LOG.info("Score of iteration '{}' was '{}'", 1, score)
    if (score > highscore) {
        highscore = score
    }

    // Reset the game
//        gym.newGame()

    LOG.info("Finished evaluation of the network, highscore was '{}'", highscore)
}








