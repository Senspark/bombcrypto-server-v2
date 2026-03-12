package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.service.IGlobalService
import com.senspark.common.utils.ILogger
import com.senspark.lib.db.BaseDataAccess
import org.intellij.lang.annotations.Language

class PvpTournamentDataAccess(
    database: IDatabase,
    val log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IPvpTournamentDataAccess {

    override fun initialize() {
    }

    override fun getTournamentUsers(): Set<String> {
        @Language("SQL")
        val statement = """
            SELECT participant_1, participant_2 from pvp_tournament;
        """.trimIndent()

        val result = mutableSetOf<String>()
        executeQuery(statement, emptyArray()) {
            val participant1 = it.getString("participant_1")
            val participant2 = it.getString("participant_2")
            result.add(participant1)
            result.add(participant2)
        }

        return result
    }
}

interface IPvpTournamentDataAccess : IGlobalService {
    fun getTournamentUsers(): Set<String>
}