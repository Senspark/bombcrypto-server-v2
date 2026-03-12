package com.senspark.game.pvp.mcts

import kotlin.math.max

class MaximizeRewardStrategy(
    private val _playerCount: Int,
    private val _normalizer: INormalizer,
) : IBackpropagationStrategy {
    override fun update(node: INode, finalState: IState) {
        val rewards = FloatArray(_playerCount)
        (0 until _playerCount).forEach {
            rewards[it] = _normalizer.normalize(finalState.getReward(it))
        }

        var currentNode = node
        while (!currentNode.isRoot) {
            val playerIndex = currentNode.playerIndex
            currentNode.addReward(rewards[playerIndex])
            currentNode.increaseVisitCount()
            currentNode = currentNode.parent!!
        }

        // Root node.
        currentNode.increaseVisitCount()
    }
}

class MaximizeRewardDifferenceStrategy(
    private val _playerCount: Int,
    private val _normalizer: INormalizer,
) : IBackpropagationStrategy {
    override fun update(node: INode, finalState: IState) {
        val rewardDifferences = FloatArray(_playerCount)
        (0 until _playerCount).forEach { i ->
            val reward = finalState.getReward(i)
            var otherPlayerReward = Float.MIN_VALUE
            (0 until _playerCount).forEach { j ->
                if (i != j) {
                    otherPlayerReward = max(otherPlayerReward, finalState.getReward(j))
                }
            }
            rewardDifferences[i] = _normalizer.normalize(reward - otherPlayerReward)
        }

        var currentNode = node
        while (!currentNode.isRoot) {
            val playerIndex = currentNode.playerIndex
            currentNode.addReward(rewardDifferences[playerIndex])
            currentNode.increaseVisitCount()
            currentNode = currentNode.parent!!
        }

        // Root node.
        currentNode.increaseVisitCount()
    }
}