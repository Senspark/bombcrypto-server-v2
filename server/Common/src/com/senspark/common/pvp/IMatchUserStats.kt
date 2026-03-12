package com.senspark.common.pvp

interface IMatchUserStats {
    val clientType: String
    val sessionType: String
    val ip: String
    val country: String
    val countryIsoCode: String
    val isUdpEnabled: Boolean
    val isEncrypted: Boolean
    val latency: Int
    val timeDelta: Int
    val lossRate: Float
}