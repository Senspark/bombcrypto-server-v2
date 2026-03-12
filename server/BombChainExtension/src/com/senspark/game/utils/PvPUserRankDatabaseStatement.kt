package com.senspark.game.utils

import com.senspark.game.schema.TablePvPUserRank
import org.jetbrains.exposed.sql.SqlExpressionBuilder.case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.update

fun TablePvPUserRank.update(userId: Int, isWinner: Boolean, deltaPoint: Int) {
    update({ uid eq userId }) {
        it[match] = match + 1
        it[point] = case().When(this.point + deltaPoint less 0, intLiteral(0)).Else(this.point + deltaPoint)
        it[win] = win + (if (isWinner) 1 else 0)
    }
}