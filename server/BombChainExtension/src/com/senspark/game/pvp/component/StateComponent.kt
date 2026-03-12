package com.senspark.game.pvp.component

import com.senspark.game.pvp.entity.IEntity
import com.senspark.game.pvp.entity.IEntityState

class StateComponent(
    override val entity: IEntity,
    private val _stateGetter: () -> IEntityState,
) : IEntityComponent {
    val state: IEntityState get() = _stateGetter()
}