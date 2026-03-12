package com.senspark.game.pvp.mcts

class Node(
    override val parent: INode?,
    override val playerIndex: Int,
    override val action: IAction,
    override val state: IState,
) : INode {
    companion object {
        fun createRootNode(
            playerIndex: Int,
            action: IAction,
            state: IState,
        ): INode {
            return Node(null, playerIndex, action, state)
        }

        fun createNode(
            parent: INode,
            playerIndex: Int,
            action: IAction,
            state: IState,
        ): INode {
            return Node(parent, playerIndex, action, state)
        }
    }

    private var _visitCount = 0
    private var _reward = 0f
    private var _children = mutableListOf<INode>()
    private var _actions = emptyList<IAction>()

    override val isRoot = parent == null
    override val isExpanded: Boolean
        get() = _children.isNotEmpty() && _children.size == _actions.size

    override val isTerminal = state.isTerminal
    override val visitCount get() = _visitCount
    override val reward get() = _reward
    override val winRate get() = if (_visitCount == 0) 0f else _reward / _visitCount

    override fun increaseVisitCount() {
        ++_visitCount
    }

    override fun addReward(amount: Float) {
        _reward += amount
    }

    override fun expand(): INode {
        require(!isExpanded) { "Already expanded" }
        if (_actions.isEmpty()) {
            _actions = state.actions.shuffled()
            _children = ArrayList(_actions.size)
        }
        val nextAction = _actions[_children.size]
        val nextState = state.clone()
        nextState.applyAction(nextAction)
        val child = createNode(
            this,
            nextState.nextPlayerIndex,
            nextAction,
            nextState,
        )
        _children.add(child)
        return child
    }

    override fun selectBestChild(evaluator: (node: INode) -> Float): INode {
        return _children.maxBy { evaluator(it) }
    }
}