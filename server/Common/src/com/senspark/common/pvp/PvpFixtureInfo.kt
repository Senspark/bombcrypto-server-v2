package com.senspark.common.pvp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PvpFixtureInfo(
    @SerialName("matches") override val matches: List<IPvpFixtureMatchInfo>,
) : IPvpFixtureInfo