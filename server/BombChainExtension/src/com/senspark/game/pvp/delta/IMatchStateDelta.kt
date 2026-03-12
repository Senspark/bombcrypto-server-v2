package com.senspark.game.pvp.delta

interface IMatchStateDelta {
    val hero: List<IHeroStateDelta>
    val bomb: List<IBombStateDelta>
    val block: List<IBlockStateDelta>
}