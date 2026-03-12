package com.senspark.game.service

fun postgreSQLDatabaseStatement() = DatabaseStatement(
    queryEvent = """
        SELECT id,
               name_event,
               EXTRACT(EPOCH FROM start_date) * 1000 AS start_date,
               EXTRACT(EPOCH FROM end_date) * 1000   AS end_date
        FROM config_event;
    """.trimIndent(),
    queryLogPlayPvP = """
        WITH result AS (SELECT *, CASE WHEN user_name_1 <> ? THEN user_name_1 ELSE user_name_2 END AS colTmp
                        FROM log_play_pvp
                        WHERE user_name_1 = ?
                           OR user_name_2 = ?)
        SELECT r.*, u.name AS opponentName
        FROM result AS r
                 INNER JOIN "user" AS u ON u.user_name = r.colTmp;
    """.trimIndent(),
    queryPvPRank = """
        WITH result AS (SELECT * FROM user_pvp_rank_ss_%d WHERE rank IS NOT NULL ORDER BY rank ASC)
        SELECT r.*,
               CASE
                   WHEN u.name IS NOT NULL THEN u.name
                   WHEN u.second_username IS NOT NULL THEN u.second_username
                   ELSE
                       CASE
                           WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                     SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                           ELSE u.user_name END
                   END as name
        FROM result AS r
                 INNER JOIN "user" AS u ON r.uid = u.id_user;
    """.trimIndent(),
    queryPvPRankingPoint = "SELECT point FROM user_pvp_rank_ss_%d WHERE uid = ?",
    updateData = "CALL reset_data(%s, %s)",
    updatePvPRankingPoint = """
        INSERT INTO user_pvp_rank_ss_%d (uid, rank, point, total_match, win_match, user_type)
        SELECT ?, (SELECT nextval('user_pvp_rank_ss_%d_seq')) AS rank , ?, 0, 0, ?
    """.trimIndent(),
    queryPvpRankingConfig = "SELECT * FROM config_pvp_ranking",
    addUserReward = "SELECT fn_add_user_reward(?, ?, ?, ?, ?);",
    subUserReward = "SELECT fn_sub_user_reward(?, ?, ?, ?, ?);",
)