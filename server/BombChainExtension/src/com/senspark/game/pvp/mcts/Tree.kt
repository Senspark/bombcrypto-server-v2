package com.senspark.game.pvp.mcts

class Tree(
    private val _maxPlayOuts: Int,
    private val _selectionStrategy: ISelectionStrategy,
    private val _backpropagationStrategy: IBackpropagationStrategy,
    private val _finalSelectionStrategy: ISelectionStrategy,
) : ITree {
    override fun computeAction(root: INode): IAction {
        repeat(_maxPlayOuts) {
            fourSteps(root)
        }
        val bestChild = _finalSelectionStrategy.select(root)
        return bestChild.action
    }

    private fun fourSteps(root: INode) {
        val bestNode = select(root)
        val expandedNode = expand(bestNode)
        val finalState = simulate(expandedNode)
        backpropagation(expandedNode, finalState)
    }

    private fun select(root: INode): INode {
        // Current visiting node.
        var currentNode = root

        // Selection.
        // Recursively select the best child of fully expanded nodes.
        // Ignore terminal nodes because they don't have children.
        while (currentNode.isExpanded && !currentNode.isTerminal) {
            currentNode = _selectionStrategy.select(currentNode)
        }

        return currentNode
    }

    private fun expand(bestNode: INode): INode {
        if (bestNode.isTerminal) {
            // Current node is a terminal node.
            // No need to expand.
            return bestNode
        }

        // Expansion.
        return bestNode.expand()
    }

    private fun simulate(expandedNode: INode): IState {
        if (expandedNode.isTerminal) {
            // Current node is a terminal node.
            // No need to simulate.
            return expandedNode.state
        }
        val state = expandedNode.state
        while (!state.isTerminal) {
            state.applyRandomAction()
        }
        return state
    }

    private fun backpropagation(expandedNode: INode, finalState: IState) {
        _backpropagationStrategy.update(expandedNode, finalState)
    }
}