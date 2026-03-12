package com.senspark.common.data

interface IBombRank {
    val bombRank: Int
    val startPoint: Int
    val endPoint: Int
    val name: String
    val winPoint: Int
    val loosePoint: Int
    val minMatches: Int
    val decayPoint: Int
}