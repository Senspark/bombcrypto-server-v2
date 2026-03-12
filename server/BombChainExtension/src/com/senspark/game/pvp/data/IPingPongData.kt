package com.senspark.game.pvp.data

interface IPingPongData {
    val requestId: Int
    val latencies: List<Int>
    val timeDelta: List<Int>
    val lossRates: List<Float>
}