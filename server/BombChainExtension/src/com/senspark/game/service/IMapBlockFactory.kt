package com.senspark.game.service

interface IMapBlockFactory<TBlockType, TMapBlockData> {
    fun createMapBlockData(health: Int, x: Int, y: Int, type: TBlockType): TMapBlockData
}