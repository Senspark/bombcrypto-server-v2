package com.senspark.game.pvp.component

import com.senspark.game.pvp.entity.IEntity

interface IEntityComponent {
    /** Gets the associated entity. */
    val entity: IEntity
}