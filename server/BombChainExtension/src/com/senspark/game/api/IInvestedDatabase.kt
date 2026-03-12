package com.senspark.game.api

interface IInvestedDatabase {
    fun query(username: String): Float
}