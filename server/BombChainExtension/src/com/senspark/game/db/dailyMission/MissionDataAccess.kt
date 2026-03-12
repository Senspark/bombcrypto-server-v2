package com.senspark.game.db.dailyMission

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserMission
import com.senspark.game.data.model.user.UserMissionRewardReceived
import com.senspark.game.declare.customEnum.MissionType
import com.senspark.lib.db.BaseDataAccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MissionDataAccess(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IMissionDataAccess {

    override fun initialize() {
    }

    override fun loadMission(uid: Int): MutableMap<String, UserMission> {
        val statement = """
            SELECT *,
                   EXTRACT(EPOCH FROM modify_date) * 1000 AS modify_date_milliseconds
            FROM user_mission
            WHERE uid = ?
              AND (NOT is_daily_mission OR date = DATE(NOW() AT TIME ZONE 'utc'));
        """.trimIndent()
        val params = arrayOf<Any?>(uid)
        val result = mutableMapOf<String, UserMission>()
        database.createQueryBuilder()
            .addStatement(statement, params)
            .executeQuery {
                val item = UserMission.fromResultSet(it)
                result[item.missionCode] = item
            }
        return result
    }

    override fun saveCompleteMission(
        uid: Int,
        missionType: MissionType,
        missionCode: String,
        numberMission: Int,
        completedMission: Int
    ) {
        val statement = """
            INSERT INTO user_mission(uid,
                                     date,
                                     type,
                                     is_daily_mission,
                                     mission_code,
                                     number_mission,
                                     completed_mission,
                                     is_received_reward,
                                     modify_date)
            VALUES (?,
                    date(NOW() AT TIME ZONE 'utc'),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    0,
                    NOW() AT TIME ZONE 'utc')
            ON CONFLICT (uid, mission_code)
                DO UPDATE SET number_mission= CASE
                                                  WHEN excluded.is_daily_mission
                                                      AND user_mission.date = DATE(NOW() AT TIME ZONE 'utc')
                                                      THEN user_mission.number_mission
                                                  ELSE excluded.number_mission END,
                              completed_mission = CASE
                                                      WHEN excluded.is_daily_mission AND
                                                           user_mission.date <> DATE(NOW() AT TIME ZONE 'utc')
                                                          THEN excluded.number_mission
                                                      ELSE user_mission.completed_mission + excluded.completed_mission END,
                              is_received_reward = CASE
                                                       WHEN excluded.is_daily_mission AND
                                                            user_mission.date <> DATE(NOW() AT TIME ZONE 'utc')
                                                           THEN 0
                                                       ELSE user_mission.is_received_reward END,
                              date = CASE
                                         WHEN excluded.is_daily_mission
                                             THEN (NOW() AT TIME ZONE 'utc')
                                         ELSE user_mission.date END,
                              modify_date = NOW() AT TIME ZONE 'utc';
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            missionType.name,
            missionType.isDailyMission,
            missionCode,
            numberMission,
            completedMission
        )
        database.createQueryBuilder(true)
            .addStatement(statement, params)
            .executeUpdate()
    }

    override fun saveReceivedReward(uid: Int, missionCode: String, rewardsReceived: List<AddUserItemWrapper>) {
        val statement = """
            UPDATE user_mission
            SET is_received_reward = 1,
                rewards_received   = ?::jsonb
            WHERE uid = ?
              AND mission_code = ?
              AND (NOT is_daily_mission OR date = Date(NOW() AT TIME ZONE 'utc'));
        """.trimIndent()
        val params = arrayOf<Any?>(Json.encodeToString(rewardsReceived.map {
            UserMissionRewardReceived(
                it.item,
                it.quantity
            )
        }), uid, missionCode)
        val statement2 = """
            INSERT INTO log_user_daily_mission(uid, date, mission_code)
            VALUES (?, NOW() AT TIME ZONE 'utc', ?);
        """.trimIndent()
        val params2 = arrayOf<Any?>(uid, missionCode)
        database.createQueryBuilder()
            .addStatementUpdate(statement, params)
            .addStatementUpdate(statement2, params2)
            .executeUpdate()
    }

    override fun checkUserAchievement(uid: Int, currentPvpRewardSeason: Int) {
        val statement = """CALL sp_check_user_achievement(?, ?)"""
        database.createQueryBuilder()
            .addStatement(statement, arrayOf(uid, currentPvpRewardSeason))
            .executeUpdate()
    }
}