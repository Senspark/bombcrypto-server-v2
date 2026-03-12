package com.senspark.game.pvp.mcts

import kotlin.math.ln
import kotlin.math.sqrt

class SelectMaxChildStrategy : ISelectionStrategy {
    override fun select(node: INode): INode {
        return node.selectBestChild {
            it.winRate
        }
    }
}

class SelectRobustChildStrategy : ISelectionStrategy {
    override fun select(node: INode): INode {
        return node.selectBestChild {
            it.visitCount.toFloat()
        }
    }
}

class SelectSecureChildStrategy(
    private val _a: Float = 1f
) : ISelectionStrategy {
    override fun select(node: INode): INode {
        return node.selectBestChild {
            val visitCount = it.visitCount
            require(visitCount > 0) { "Zero visit count" }
            // Formula:
            // v + A / sqrt(n)
            // v = reward
            // A = parameter, usually equal to 1.
            // n = visit count.
            val value = it.reward + _a * sqrt(visitCount.toFloat())
            value
        }
    }
}

class UpperConfidenceBoundStrategy(
    private val _c: Float = 2f,
) : ISelectionStrategy {
    override fun select(node: INode): INode {
        if (!node.isExpanded) {
            return node.expand()
        }
        val logValue = ln(node.visitCount.toFloat())
        return node.selectBestChild {
            val visitCount = it.visitCount
            require(visitCount > 0) { "Zero visit count" }
            val invertedVisitCount = 1f / visitCount
            val exploit = it.reward * invertedVisitCount
            val explore = sqrt(_c * logValue * invertedVisitCount)
            val value = exploit + explore
            value
        }
    }
}