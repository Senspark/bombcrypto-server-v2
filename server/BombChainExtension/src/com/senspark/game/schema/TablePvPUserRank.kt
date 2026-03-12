package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

class TablePvPUserRank(seasonId: Int) : Table("user_pvp_rank_ss_$seasonId") {
    val uid = integer("uid")
    val match = integer("total_match")
    val point = integer("point")
    val win = integer("win_match")
}