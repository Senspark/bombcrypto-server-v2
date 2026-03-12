package com.senspark.game.pvp.delta

class MatchStateDelta(
    override val hero: List<IHeroStateDelta>,
    override val bomb: List<IBombStateDelta>,
    override val block: List<IBlockStateDelta>,
) : IMatchStateDelta