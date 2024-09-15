package art.vilolon.backgammon.game.rule

import art.vilolon.backgammon.game.entity.GAvailableMoves
import art.vilolon.backgammon.game.entity.GChecker
import art.vilolon.backgammon.game.entity.GDice
import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.entity.HolePosition
import art.vilolon.backgammon.game.entity.GPlayer
import art.vilolon.backgammon.game.entity.GMovePosition
import art.vilolon.backgammon.game.utils.Logger
import java.util.Objects
import kotlin.math.abs
import kotlin.random.Random

private const val TAG = "GameRule"

class GameRule constructor(
    loggerFactory: Logger.Factory,
) {
    private val logger: Logger = loggerFactory.create(TAG)

    fun getAvailableMoves(
        player: GPlayer,
        opponent: GPlayer,
        allDices: List<GDice>,
        turnPlayer: Int,
        checker: GChecker,
    ): GAvailableMoves {
        val dices = allDices.filter { !it.isUsed }
        val availableMoves = buildList {
            val checkedPosition2Moves = mutableMapOf<GChecker, Set<GMovePosition>>()
            player.checkers.forEach { checker ->
                checkedPosition2Moves.keys.find { it.position == checker.position }?.let { alreadyComputeChecker ->
                    val alreadyComputeMoves = checkNotNull(checkedPosition2Moves[alreadyComputeChecker])
                    checkedPosition2Moves[checker] = alreadyComputeMoves
                    addAll(alreadyComputeMoves)
                    return@forEach
                }
                addAll(
                    getAvailableHoleForChecker(
                        checker = checker,
                        player = player,
                        opponent = opponent,
                        dices = dices,
                        turnPlayer = turnPlayer,
                    ).also { moves ->
                        checkedPosition2Moves[checker] = moves
                    }
                )
            }
        }
            .distinctBy { Objects.hash(it.checker.id, it.toPosition) }
            .applyLongerMoveRule(dices, turnPlayer, player.checkers)

        return GAvailableMoves(
            availableMoves.filter { moves ->
                moves.checker.position == checker.position
            }
                .toSet()
//                .onEach {
//                    logger.w("AvailableMoves: $it")
//                }
        )
    }

    fun getAvailableCheckersForPlayer(
        player: GPlayer,
        opponent: GPlayer,
        allDices: List<GDice>,
        turnPlayer: Int,
    ): List<GChecker> {
        val dices = allDices.filter { !it.isUsed }
        val availableChecker = buildList {
            val checkedPosition2Moves = mutableMapOf<GChecker, Set<GMovePosition>>()
            player.checkers.forEach { checker ->
                checkedPosition2Moves.keys.find { it.position == checker.position }?.let { alreadyComputeChecker ->
                    val alreadyComputeMoves = checkNotNull(checkedPosition2Moves[alreadyComputeChecker])
                    checkedPosition2Moves[checker] = alreadyComputeMoves
                    addAll(alreadyComputeMoves)
                    return@forEach
                }
                addAll(
                    getAvailableHoleForChecker(
                        checker = checker,
                        player = player,
                        opponent = opponent,
                        dices = dices,
                        turnPlayer = turnPlayer,
                    ).also { moves ->
                        checkedPosition2Moves[checker] = moves
                    }
                )
            }
        }
            .distinctBy { Objects.hash(it.checker.id, it.toPosition) }
            .applyLongerMoveRule(dices, turnPlayer, player.checkers)
            .map { moves ->
                moves.checker
            }
        return player.checkers.filter { pChecker ->
            availableChecker.any { availableChecker ->
                availableChecker.position == pChecker.position
            }
        }
    }

    fun getMovesAndCheckersForPlayer(
        player: GPlayer,
        opponent: GPlayer,
        allDices: List<GDice>,
        turnPlayer: Int,
    ): Map<GChecker, Set<GMovePosition>> {
        val dices = allDices.filter { !it.isUsed }
        val availableChecker = buildList {
            val checkedPosition2Moves = mutableMapOf<GChecker, Set<GMovePosition>>()
            player.checkers.forEach { checker ->
                checkedPosition2Moves.keys.find { it.position == checker.position }?.let { alreadyComputeChecker ->
                    val alreadyComputeMoves = checkNotNull(checkedPosition2Moves[alreadyComputeChecker])
                    checkedPosition2Moves[checker] = alreadyComputeMoves
                    addAll(alreadyComputeMoves)
                    return@forEach
                }
                addAll(
                    getAvailableHoleForChecker(
                        checker = checker,
                        player = player,
                        opponent = opponent,
                        dices = dices,
                        turnPlayer = turnPlayer,
                    ).also { moves ->
                        checkedPosition2Moves[checker] = moves
                    }
                )
            }
        }
            .distinctBy { Objects.hash(it.checker.id, it.toPosition) }
            .applyLongerMoveRule(dices, turnPlayer, player.checkers)

        val checkers2Moves = mutableMapOf<GChecker, MutableSet<GMovePosition>>()
        availableChecker.forEach { move ->
            val moves = checkers2Moves[move.checker]?.apply { add(move) } ?: mutableSetOf(move)
            checkers2Moves[move.checker] = moves
        }
        return checkers2Moves
    }

    fun getNewPosition(
        game: GGame,
        newPosition: HolePosition,
        oldPosition: HolePosition,
    ): HolePosition = when (game.turnPlayer) {
        P1 -> newPosition
        P2 -> if (newPosition > P2_JUMP_BOARD) {
            logger.d("getNewPosition newPosition: $newPosition oldPosition:$oldPosition go to: ${newPosition - oldPosition}")
            newPosition - oldPosition
        } else {
            newPosition
        }

        else -> throw IllegalArgumentException("wrong p:${game.turnPlayer}")
    }

    /**
     * If moves can be made according to either one die or the other,
     * but not both, the higher number must be used.
     * If one die is unable to be moved,
     * but such a move is made possible by the moving of the other die,
     * that move is compulsory.
     * */
    private fun List<GMovePosition>.applyLongerMoveRule(
        dices: List<GDice>,
        turnPlayer: Int,
        playerCheckers: List<GChecker>,
    ): List<GMovePosition> {
        if (isEmpty()) return this
        if (dices.size != 2 || dices[0].value == dices[1].value) return this
        if (isOneNotHome(playerCheckers, turnPlayer)) return this
        logger.v("Longer move rule process moves, ${dices.joinToString()}")
        val diceValue2UseCount = mutableMapOf(
            dices[0] to 0,
            dices[1] to 0
        )
        forEach { move ->
            move.dices.forEach { dice ->
                diceValue2UseCount[dice] = diceValue2UseCount[dice]!!.inc()
            }
        }

        val singleDices = diceValue2UseCount.filter { it.value == 1 }.keys

        return when (singleDices.size) {
            1 -> {
                val singleDice = singleDices.first()
                val moveOnlyUseDice = checkNotNull(find { it.dices.contains(singleDice) })
                when {
                    moveOnlyUseDice.dices.indexOf(singleDice) == 0 -> {
                        val availableMoves = filter { oneOfAllMove ->
                            moveOnlyUseDice == oneOfAllMove || oneOfAllMove.checker != moveOnlyUseDice.checker
                        }
                        logger.v("Longer move rule index 0")
                        availableMoves
                    }

                    moveOnlyUseDice.dices.indexOf(singleDice) == 1 -> {
                        val availableMoves = filter { oneOfAllMove ->
                            moveOnlyUseDice.checker == oneOfAllMove.checker
                                    || oneOfAllMove.dices.any { dice -> singleDice.value == dice.value }
                        }
                        logger.v("Longer move rule index 1")
                        availableMoves
                    }

                    else -> throw IllegalStateException(
                        "Longer move rule, wrong index:${moveOnlyUseDice.dices.indexOf(singleDice)}"
                    )
                }
            }

            2 -> {
                logger.v("Longer move, both dices is unique")
                val firstSingleDice = singleDices.first()
                val lastSingleDice = singleDices.last()
                val firstMoveOnlyUseDice = checkNotNull(find { it.dices.contains(firstSingleDice) })
                val lastMoveOnlyUseDice = checkNotNull(find { it.dices.contains(lastSingleDice) })
                if (firstMoveOnlyUseDice.checker == lastMoveOnlyUseDice.checker) {
                    val maxDice = if (firstMoveOnlyUseDice.dices.first().value > lastMoveOnlyUseDice.dices.first().value)
                        firstMoveOnlyUseDice.dices.first() else lastMoveOnlyUseDice.dices.first()
                    val availableMoves = filter { oneOfAllMove ->
                        oneOfAllMove.dices.any { dice -> maxDice.value == dice.value }
                    }
                    availableMoves
                } else {
                    this
                }
            }

            else -> {
                val hasBlockedDice = all { first().dices.first() == it.dices.first() }
                return if (hasBlockedDice) {
                    val checkersTwoDice = filter { it.dices.size == 2 }.map { it.checker }
                    find { it.dices.size == 2 }?.let {
                        val availableMoves = filter { oneOfAllMove ->
                            oneOfAllMove.dices.size != 1
                                    || oneOfAllMove.checker in checkersTwoDice
                        }
                        logger.v("Has blocked ${first().dices.last()}")
                        availableMoves
                    } ?: this
                } else {
//  GMovePosition(checker=GChecker(id=3, position=11, highPosition=0, canMove=true, isSelected=false), toPosition=14, dices=[GDice(value=3, isUsed=false, id=2)])
//  GMovePosition(checker=GChecker(id=3, position=11, highPosition=0, canMove=true, isSelected=false), toPosition=20, dices=[GDice(value=3, isUsed=false, id=2), GDice(value=6, isUsed=false, id=3)])
//  GMovePosition(checker=GChecker(id=4, position=15, highPosition=0, canMove=true, isSelected=false), toPosition=18, dices=[GDice(value=3, isUsed=false, id=2)]) *
//  GMovePosition(checker=GChecker(id=4, position=15, highPosition=0, canMove=true, isSelected=false), toPosition=21, dices=[GDice(value=6, isUsed=false, id=3)])
//  GMovePosition(checker=GChecker(id=5, position=20, highPosition=0, canMove=true, isSelected=false), toPosition=23, dices=[GDice(value=3, isUsed=false, id=2)])
                    //todo refactor to diceCheckerMove tree

                    this
                }
            }
        }
    }

    private fun isOneNotHome(
        playerCheckers: List<GChecker>,
        turnPlayer: Int,
    ): Boolean {
        val homePositions = if (turnPlayer == P1) P1_HOME_POSITIONS else P2_HOME_POSITIONS
        return playerCheckers.count { it.position in homePositions } == P_CHECKERS_COUNT - 1
    }

    private fun getAvailableHoleForChecker(
        checker: GChecker,
        player: GPlayer,
        opponent: GPlayer,
        dices: List<GDice>,
        turnPlayer: Int,
    ): Set<GMovePosition> {

        val dicesV1Positions = getHolePosition(
            checker = checker,
            player = player,
            opponent = opponent,
            dices = dices,
            turnPlayer = turnPlayer,
            revers = false,
        )

        val dicesV2Positions = getHolePosition(
            checker = checker,
            player = player,
            opponent = opponent,
            dices = dices,
            turnPlayer = turnPlayer,
            revers = true,
        )
        return (dicesV1Positions + dicesV2Positions)
    }

    private fun getHolePosition(
        checker: GChecker,
        player: GPlayer,
        opponent: GPlayer,
        dices: List<GDice>,
        turnPlayer: Int,
        revers: Boolean,
    ): Set<GMovePosition> {
        val dicesAllVars = if (revers) dices.filter { !it.isUsed }.reversed() else dices.filter { !it.isUsed }

        return when {
            player.allAtHome -> getHomeHolePosition(
                checker = checker,
                player = player,
                opponent = opponent,
                dices = dicesAllVars,
                turnPlayer = turnPlayer,
            )

            else -> getMiddleHolePosition(
                checker = checker,
                player = player,
                opponent = opponent,
                dices = dicesAllVars,
                turnPlayer = turnPlayer,
            )
        }
    }

    private fun getMiddleHolePosition(
        checker: GChecker,
        player: GPlayer,
        opponent: GPlayer,
        dices: List<GDice>,
        turnPlayer: Int,
    ): Set<GMovePosition> {
        logger.d("Middle game check checker: $checker")
        return buildSet {
            val hasTookFromHead = player.tookHead &&
                    ((checker.position == P1_HEAD && turnPlayer == P1) || (checker.position == P2_HEAD && turnPlayer == P2))
            if (hasTookFromHead) {
                if (dices.all { it.value == 6 || it.value == 4 || it.value == 3 }) {
                    addDoubleFirstMoveIfJackpot(dices, turnPlayer, player, checker)
                }
                return@buildSet
            }

            val accMoves: Array<GMovePosition?> = Array(dices.size) { null }

            dices.forEachIndexed { index, dice ->
                if (index == 0) {
                    val toPosition = checker.position + dice.value
                    val moveTo = if (toPosition > P2_JUMP_BOARD && turnPlayer == P2) toPosition - P2_JUMP_BOARD else toPosition
                    val isCollision = checkCollision(opponent, moveTo)
                    if (turnPlayer == P1 && moveTo <= P1_BOARD_OUT) {
                        if (!isCollision && isAllowedBlock(checker, player, moveTo, opponent, turnPlayer)) {
                            val move = GMovePosition(
                                checker = checker,
                                toPosition = moveTo,
                                dices = listOf(dice)
                            )
                            add(move)
                            logger.v("p$turnPlayer checker:$checker can move:$move")
                            accMoves[0] = move
                            logger.v("p1 set accValues: ${accMoves[0]}")
                        } else {
                            logger.d("p1 checker:$checker collisions or block to:$moveTo dice:$dice")
                        }
                    } else if (turnPlayer == P1) {
                        logger.d("Don't home and out p1 checker:${checker} dice:$dice")
                    }

                    if (turnPlayer == P2 && moveTo <= P2_BOARD_OUT && checker.position in BOTTOM_ROW) {
                        if (!isCollision && isAllowedBlock(checker, player, moveTo, opponent, turnPlayer)) {
                            val move = GMovePosition(
                                checker = checker,
                                toPosition = moveTo,
                                dices = listOf(dice)
                            )
                            add(move)
                            logger.v("p$turnPlayer checker:$checker can move:$move")
                            accMoves[0] = move
                            logger.v("p2 BOTTOM_ROW set accValues: ${accMoves[0]}")
                        } else {
                            logger.d("p2 checker:$checker collisions or block to:$moveTo dice:$dice")
                        }
                    } else if (turnPlayer == P2) {
                        logger.d("Don't home and out p2 checker:${checker} dice:$dice")
                    }

                    if (turnPlayer == P2 && checker.position in TOP_ROW) {
                        if (!isCollision && isAllowedBlock(checker, player, moveTo, opponent, turnPlayer)) {
                            val move = GMovePosition(
                                checker = checker,
                                toPosition = moveTo,
                                dices = listOf(dice)
                            )
                            add(move)
                            logger.v("p2 checker:$checker can move to:$moveTo dice:$dice")
                            accMoves[0] = move
                            logger.v("p2 TOP_ROW set accValues: ${accMoves[0]}")
                        } else {
                            logger.d("p2 checker:$checker collisions or block to:$moveTo dice:$dice")
                        }
                    } else if (turnPlayer == P2) {
                        logger.d("Don't home and out p2 checker:${checker} dice:$dice")
                    }
                }

                // For many dices
                if (index > 0) {
                    accMoves[index] = accMoves[index - 1]?.let {
                        val toPosition = it.toPosition + dice.value
                        val moveTo =
                            if (toPosition > P2_JUMP_BOARD && turnPlayer == P2) toPosition - P2_JUMP_BOARD else toPosition
                        if (turnPlayer == P1 && moveTo > P1_BOARD_OUT) return@let null

                        val isCollision = checkCollision(opponent, moveTo)
                        if (isCollision) return@let null

                        val p2Out = turnPlayer == P2 && accMoves.any { accMove ->
                            // jump from top row to bottom in one move and out
                            accMove?.toPosition in BOTTOM_ROW && moveTo in TOP_ROW
                        }
                        if (p2Out) return@let null

                        val isAllowedBlock = isAllowedBlock(checker, player, moveTo, opponent, turnPlayer)
                        if (!isAllowedBlock) return@let null

                        it.copy(
                            toPosition = moveTo,
                            dices = it.dices
                                .toMutableList()
                                .apply { add(dice) },
                        ).also {
                            logger.d("p$turnPlayer accValues index:${index} is $it, $moveTo dice:$dice")
                        }
                    }
//                logger.d( "Don't home and NOT out p$turnPlayer checker:${checker.position} to:${outPosition} dice:${dice.value}")
                }
            }
            for (i in 1 until accMoves.size) {
                accMoves[i]?.let { move ->
                    add(move)
                    logger.d("p$turnPlayer add acc value $move")
                }
            }
        }
    }

    private fun MutableSet<GMovePosition>.addDoubleFirstMoveIfJackpot(
        dices: List<GDice>,
        turnPlayer: Int,
        player: GPlayer,
        checker: GChecker,
    ) {
        if (turnPlayer == P1) {
            if ((dices.all { it.value == 6 })
                && (player.checkers.count { it.position == P1_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P1_HEAD + 6 } == 1)
            ) {
                val dice = dices.first()
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P1_HEAD + 6,
                        dices = listOf(dice)
                    )
                )
            } else if ((dices.all { it.value == 4 })
                && (player.checkers.count { it.position == P1_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P1_HEAD + 4 * 2 } == 1)
            ) {
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P1_HEAD + 4 * 2,
                        dices = dices
                    )
                )
            } else if ((dices.all { it.value == 3 })
                && (player.checkers.count { it.position == P1_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P1_HEAD + 3 * 3 } == 1)
            ) {
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P1_HEAD + 3,
                        dices = dices
                    )
                )
            }
        } else if (turnPlayer == P2) {
            if ((dices.all { it.value == 6 })
                && (player.checkers.count { it.position == P2_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P2_HEAD + 6 } == 1)
            ) {
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P2_HEAD + 6,
                        dices = listOf(dices.first())
                    )
                )
            } else if ((dices.all { it.value == 4 })
                && (player.checkers.count { it.position == P2_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P2_HEAD + 4 * 2 } == 1)
            ) {
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P2_HEAD + 4 * 2,
                        dices = dices
                    )
                )
            } else if ((dices.all { it.value == 3 })
                && (player.checkers.count { it.position == P2_HEAD } == P_CHECKERS_COUNT - 1)
                && (player.checkers.count { it.position == P2_HEAD + 3 * 3 } == 1)
            ) {
                add(
                    GMovePosition(
                        checker = checker,
                        toPosition = P2_HEAD + 3,
                        dices = dices
                    )
                )
            }
        }
    }

    private fun checkCollision(opponent: GPlayer, moveTo: Int) = opponent.checkers.filter { opponentChecker ->
        if (opponent.allAtHome)
            opponentChecker.position in P1_HOME_POSITIONS || opponentChecker.position in P2_HOME_POSITIONS
        else true
    }.any { opponentChecker ->
        opponentChecker.position == moveTo
    }

    private fun isAllowedBlock(
        checker: GChecker,
        player: GPlayer,
        moveTo: HolePosition,
        opponent: GPlayer,
        turnPlayer: Int,
    ): Boolean {
        var maxPosition = 0
        when {
            player.checkers.any { it.position == moveTo } -> return true

            turnPlayer == P1 && (opponent.checkers.any { it.position in P2_HOME_POSITIONS }) -> return true

            turnPlayer == P2 && (opponent.checkers.any { it.position in P1_HOME_POSITIONS }) -> return true

            (turnPlayer == P1) && (moveTo in TOP_ROW) -> {
                if (opponent.checkers.any { it.position in BOTTOM_ROW }) return true
            }

            (turnPlayer == P1) && (moveTo in BOTTOM_ROW) -> {
                maxPosition = opponent.checkers.filter { it.position in BOTTOM_ROW }.maxOfOrNull { it.position } ?: 0
                if (maxPosition > moveTo) return true
            }

            else -> {
                maxPosition = opponent.checkers.maxOf { it.position }
                if (maxPosition > moveTo) return true
            }
        }

        var incBlockLength = 1
        while (incBlockLength < BLOCK_LINE_LENGTH) {
            val nextCheckPos = moveTo + incBlockLength
            val checkPosition = when {
                nextCheckPos > P1_BOARD_OUT -> nextCheckPos - P1_BOARD_OUT
                else -> nextCheckPos
            }

            if (player.checkers.any { c ->
                    c.position == checkPosition
                }
            ) {
                incBlockLength++
            } else {
                break
            }
        }

        if (incBlockLength >= BLOCK_LINE_LENGTH) {
            logger.d("Don't allowed block on:$moveTo, length:$incBlockLength, maxPosition:$maxPosition")
            return false
        }

        var decBlockLength = -1
        var decJumpOffset = 0
        while (decBlockLength > -BLOCK_LINE_LENGTH) {
            val prevCheckPos = moveTo + decBlockLength
            val checkPosition = when {
                prevCheckPos < 1 -> {
                    if (decJumpOffset == 0) decJumpOffset = -decBlockLength
                    P1_BOARD_OUT + decBlockLength + decJumpOffset
                }

                else -> prevCheckPos
            }

            if (player.checkers.any { c ->
                    c.position == checkPosition
                            && !(c == checker && player.checkers.count { it.position == checker.position } == 1)
                }
            ) {
                decBlockLength--
            } else {
                break
            }
        }

        val blockLineLength = incBlockLength + abs(decBlockLength) - 1

        if (blockLineLength >= BLOCK_LINE_LENGTH) {
            logger.d("Don't allowed block on:$moveTo, length:$blockLineLength, maxPosition:$maxPosition")
            return false
        }

        return true
    }

    private fun getHomeHolePosition(
        checker: GChecker,
        player: GPlayer,
        opponent: GPlayer,
        dices: List<GDice>,
        turnPlayer: Int,
    ): Set<GMovePosition> {

        if (checker.position == P2_HEAD || checker.position == P1_HEAD) {
            logger.d("Finished p$turnPlayer checker:${checker.position}")
            return emptySet()
        }

        return buildSet {
            val accMoves: Array<GMovePosition?> = Array(dices.size) { null }
            val lastCheckerPosition = player.checkers.filter { checker ->
                checker.position in P1_HOME_POSITIONS || checker.position in P2_HOME_POSITIONS
            }.minOf { it.position }
            val isLastCheckerAtHome = checker.position == lastCheckerPosition

            dices.forEachIndexed { index, dice ->
                if (index == 0) {
                    val toPosition = checker.position + dice.value

                    if (turnPlayer == P1 && toPosition > P1_BOARD_OUT && isLastCheckerAtHome) {
                        add(
                            GMovePosition(
                                checker = checker,
                                toPosition = P1_END,
                                dices = listOf(dice)
                            )
                        )
                        logger.d("Add p$turnPlayer last home checker:${checker} to $P1_END dice:$dice")
                    }
                    if (turnPlayer == P2 && toPosition > P2_BOARD_OUT && isLastCheckerAtHome) {
                        add(
                            GMovePosition(
                                checker = checker,
                                toPosition = P2_END,
                                dices = listOf(dice)
                            )
                        )
                        logger.d("Add p$turnPlayer last home checker:${checker} to $P2_END dice:$dice")
                    }

                    if (turnPlayer == P1 && toPosition == P1_BOARD_OUT + 1) {
                        add(
                            GMovePosition(
                                checker = checker,
                                toPosition = P1_END,
                                dices = listOf(dice)
                            )
                        )
                        logger.d("Add p$turnPlayer home checker:${checker} to $P1_END dice:$dice")
                    }
                    if (turnPlayer == P2 && toPosition == P2_BOARD_OUT + 1) {
                        logger.d("Add p$turnPlayer home checker:${checker} to $P2_END dice:$dice")
                        add(
                            GMovePosition(
                                checker = checker,
                                toPosition = P2_END,
                                dices = listOf(dice)
                            )
                        )
                    }

                    val isNotCollision = !opponent.checkers.any { it.position == toPosition }
                    if ((turnPlayer == P1) && isNotCollision && (toPosition <= P1_BOARD_OUT)) {
                        val move = GMovePosition(
                            checker = checker,
                            toPosition = toPosition,
                            dices = listOf(dice)
                        )
                        add(move)
                        accMoves[0] = move
                        logger.v("p1 set accValues: ${accMoves[0]}")
                    }
                    if ((turnPlayer == P2) && isNotCollision && (toPosition <= P2_BOARD_OUT)) {
                        val move = GMovePosition(
                            checker = checker,
                            toPosition = toPosition,
                            dices = listOf(dice)
                        )
                        add(move)
                        accMoves[0] = move
                        logger.v("p2 set accValues: ${accMoves[0]}")
                    }
                }

                //acc moves
                if (index > 0) {
                    accMoves[index] = accMoves[index - 1]?.let { prevMovePosition ->
                        val toPosition = prevMovePosition.toPosition + dice.value
                        val isCollision = opponent.checkers.any { opponent -> opponent.position == toPosition }

                        when {
                            (turnPlayer == P1) && !isCollision && (toPosition in P1_HOME_POSITIONS) -> {
                                logger.v("p$turnPlayer add accValues 1: ${toPosition} dice:$dice")
                                prevMovePosition.copy(
                                    toPosition = toPosition,
                                    dices = prevMovePosition.dices
                                        .toMutableList()
                                        .apply { add(dice) },
                                )
                            }

                            (turnPlayer == P2) && !isCollision && (toPosition in P2_HOME_POSITIONS) -> {
                                logger.v("p$turnPlayer add accValues 1: ${toPosition} dice:$dice")
                                prevMovePosition.copy(
                                    toPosition = toPosition,
                                    dices = prevMovePosition.dices
                                        .toMutableList()
                                        .apply { add(dice) },
                                )
                            }

                            (turnPlayer == P1) && (toPosition == P1_BOARD_OUT + 1) -> {
                                logger.v("p$turnPlayer add accValues 2: ${toPosition} dice:$dice")
                                prevMovePosition.copy(
                                    toPosition = P1_END,
                                    dices = prevMovePosition.dices
                                        .toMutableList()
                                        .apply { add(dice) },
                                )
                            }

                            (turnPlayer == P2) && (toPosition == P2_BOARD_OUT + 1) -> {
                                logger.v("p$turnPlayer add accValues 2: ${toPosition} dice:$dice")
                                prevMovePosition.copy(
                                    toPosition = P2_END,
                                    dices = prevMovePosition.dices
                                        .toMutableList()
                                        .apply { add(dice) },
                                )
                            }

                            else -> null
                        }
                    }
                }

//                logger.d( "Available checker filter: $checker")
            }
            for (i in 1 until accMoves.size) {
                accMoves[i]?.let { move ->
                    add(move)
                    logger.d("p$turnPlayer add acc value $move")
                }
            }
        }
    }

    fun isPlayerWon(game: GGame): Int? {
        if (game.player2.allAtHome && game.player2.checkers.all { it.position == P2_END }) {
            return P2
        }
        if (game.player1.allAtHome && game.player1.checkers.all { it.position == P1_END }) {
            return P1
        }
        return null
    }

    fun rollingDices(count: Int, notAllowedValue: Int? = null): List<GDice> {
        var tmp = getRandomDiceValue()
        while (tmp == notAllowedValue) {
            tmp = getRandomDiceValue()
        }
        val d1 = tmp
        val d2 = getRandomDiceValue()
        return buildList {
            add(
                GDice(
                    value = d1,
                )
            )
            if (count == 2) {
                add(
                    GDice(
                        value = d2,
                    )
                )
            }
        }
    }

    private fun getRandomDiceValue() = Random.nextInt(6) + 1
}