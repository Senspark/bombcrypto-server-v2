package com.senspark.common.constant

enum class PlayPvPMatchResult(val value: String) {
    Complete("complete"),
    Draw("draw");

    companion object {
        private val types = PlayPvPMatchResult.values().associateBy { it.value }
        fun fromString(value: String) = types[value] ?: throw Exception("Could not find play pvp match result: $value")
    }
}