package com.senspark.game.pvp.mcts


/**
 * Moves by [x, y]
 */
open class MoveAction(
    val x: Int,
    val y: Int,
) : IAction

/** Moves left. */
class MoveLeftAction : MoveAction(-1, 0)

/** Moves right. */
class MoveRightAction : MoveAction(+1, 0)

/** Moves up. */
class MoveUpAction : MoveAction(0, +1)

/** Moves down. */
class MoveDownAction : MoveAction(0, -1)

/** Plant a bomb. */
class PlantBombAction : IAction

/** Waits for delta milliseconds. */
class WaitAction(
    val delta: Int,
) : IAction