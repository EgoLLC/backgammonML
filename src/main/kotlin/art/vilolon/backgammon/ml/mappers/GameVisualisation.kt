package art.vilolon.backgammon.ml.mappers

import art.vilolon.backgammon.game.entity.GGame
import art.vilolon.backgammon.game.rule.NEW_GAME_P2AI
import art.vilolon.backgammon.game.rule.NEW_GAME_P2P
import art.vilolon.backgammon.game.rule.P1
import art.vilolon.backgammon.game.rule.P2

fun main() {
    GameVisualisation.render(NEW_GAME_P2P)
//    GameVisualisation().render(NEW_GAME_P2AI)
}

object GameVisualisation {

    fun render(game: GGame) {
        val p1CheckersToPos = game.player1.checkers.groupBy { it.position }
        val p2CheckersToPos = game.player2.checkers.groupBy { it.position }
        val dices = game.dices.values.filter { !it.isUsed }.map { it.value }.joinToString("")

        //header
//        println("dices: ${dices}")
        print("-".repeat(6))
        print("|")
        print("-".repeat(6))
        println()
        for (i in 24 downTo 19) {
            when {
                p1CheckersToPos.containsKey(i) -> print("x")
                p2CheckersToPos.containsKey(i) -> print("o")
                else -> print(" ")
            }
        }
        print("|")
        for (i in 18 downTo 13) {
            when {
                p1CheckersToPos.containsKey(i) -> print("x")
                p2CheckersToPos.containsKey(i) -> print("o")
                else -> print(" ")
            }
        }
        println()

        if (game.turnPlayer == P1) {
            print(" ")
            print(dices)
            print(" ".repeat(5 - dices.length))
        } else {
            print(" ".repeat(6))
        }
        print("|")
        if (game.turnPlayer == P2) {
            print(" ")
            print(dices)
            print(" ".repeat(5 - dices.length))
        } else {
            print(" ".repeat(6))
        }
        println()

        //bottom
        for (i in 1..6) {
            when {
                p1CheckersToPos.containsKey(i) -> print("x")
                p2CheckersToPos.containsKey(i) -> print("o")
                else -> print(" ")
            }
        }
        print("|")
        for (i in 7..12) {
            when {
                p1CheckersToPos.containsKey(i) -> print("x")
                p2CheckersToPos.containsKey(i) -> print("o")
                else -> print(" ")
            }
        }
        println()

        print("-".repeat(6))
        print("|")
        print("-".repeat(6))
        println()
        println()
    }
}

/*
GGame
  (
    vs=AI,
player1=GPlayer
    checkers=[GChecker
        (id=0, position=8, highPosition=0, canMove=true, isSelected=false), GChecker
        (id=1, position=1, highPosition=1, canMove=true, isSelected=false), GChecker
        (id=2, position=1, highPosition=2, canMove=true, isSelected=false), GChecker
        (id=3, position=1, highPosition=3, canMove=true, isSelected=false), GChecker
        (id=4, position=1, highPosition=4, canMove=true, isSelected=false), GChecker
        (id=5, position=1, highPosition=5, canMove=true, isSelected=false), GChecker
        (id=6, position=1, highPosition=6, canMove=true, isSelected=false), GChecker
        (id=7, position=1, highPosition=7, canMove=true, isSelected=false), GChecker
        (id=8, position=1, highPosition=8, canMove=true, isSelected=false), GChecker
        (id=9, position=1, highPosition=9, canMove=true, isSelected=false), GChecker
        (id=10, position=1, highPosition=10, canMove=true, isSelected=false), GChecker
        (id=11, position=1, highPosition=11, canMove=true, isSelected=false), GChecker
        (id=12, position=1, highPosition=12, canMove=true, isSelected=false), GChecker
        (id=13, position=1, highPosition=13, canMove=true, isSelected=false), GChecker
        (id=14, position=1, highPosition=14, canMove=true, isSelected=false)],
    tookHead=false,
    allAtHome=false),
player2=GPlayer(
    checkers=[GChecker
        (id=15, position=22, highPosition=0, canMove=true, isSelected=false), GChecker
        (id=16, position=13, highPosition=1, canMove=true, isSelected=false), GChecker
        (id=17, position=13, highPosition=2, canMove=true, isSelected=false), GChecker
        (id=18, position=13, highPosition=3, canMove=true, isSelected=false), GChecker
        (id=19, position=13, highPosition=4, canMove=true, isSelected=false), GChecker
        (id=20, position=13, highPosition=5, canMove=true, isSelected=false), GChecker
        (id=21, position=13, highPosition=6, canMove=true, isSelected=false), GChecker
        (id=22, position=13, highPosition=7, canMove=true, isSelected=false), GChecker
        (id=23, position=13, highPosition=8, canMove=true, isSelected=false), GChecker
        (id=24, position=13, highPosition=9, canMove=true, isSelected=false), GChecker
        (id=25, position=13, highPosition=10, canMove=true, isSelected=false), GChecker
        (id=26, position=13, highPosition=11, canMove=true, isSelected=false), GChecker
        (id=27, position=13, highPosition=12, canMove=true, isSelected=false), GChecker
        (id=28, position=13, highPosition=13, canMove=true, isSelected=false), GChecker
        (id=29, position=13, highPosition=14, canMove=true, isSelected=false)],
    tookHead=false,
    allAtHome=false),
dices=GDicesOnBoard
  leftBoard=GDices
  values=[
    GDice(value=1, isUsed=false, id=172),
    GDice(value=4, isUsed=false, id=173)
  ],
  rolling=false),
  rightBoard=GDices(
    values=[], rolling=false)),
availableMoves=GAvailableMoves
  holes=[
    GMovePosition
        checker=GChecker(
          id=15,
          position=19,
          highPosition=0,
          canMove=true,
          isSelected=false),
          toPosition=22,
        dices=[
        GDice(
          value=3,
          isUsed=false,
          id=170)])]),
turnPlayer=1,
state=Playing)
* */