package autoMine

import com.senspark.testclient.TestEnv
import io.lettuce.core.RedisClient
import java.sql.Connection
import java.sql.DriverManager

/**
 * Dọn và gieo dữ liệu cho test giá auto-mine. Ghi vào `logs.user_block_reward` nên mọi đường vào
 * đều chặn cứng ở localhost.
 */
class TestFixture(
    private val jdbcUrl: String = TestEnv.optional("TEST_PG_URL", "jdbc:postgresql://localhost:5432/bombcrypto")!!,
    private val dbUser: String = TestEnv.optional("TEST_PG_USER", "postgres")!!,
    private val dbPassword: String = TestEnv.required("TEST_PG_PASSWORD"),
    private val redisUrl: String = TestEnv.optional("TEST_REDIS_URL", "redis://localhost:6379")!!,
) {
    init {
        requireLocal(jdbcUrl)
        requireLocal(redisUrl)
    }

    private fun requireLocal(url: String) {
        val local = LOCAL_HOSTS.any { url.contains("//$it:") || url.contains("//$it/") }
        require(local) {
            "Test này XOÁ rồi GHI vào logs.user_block_reward. Chỉ chạy trên máy local. URL bị từ chối: $url"
        }
    }

    private fun <T> withDb(block: (Connection) -> T): T =
        DriverManager.getConnection(jdbcUrl, dbUser, dbPassword).use(block)

    fun findUid(userName: String): Int? = withDb { db ->
        db.prepareStatement("SELECT id_user FROM \"user\" WHERE user_name = ?").use { st ->
            st.setString(1, userName)
            st.executeQuery().use { if (it.next()) it.getInt(1) else null }
        }
    }

    /** Cửa sổ tính giá không tính hôm nay, gieo vào hôm nay thì mined_value = 0. */
    fun seedMinedBcoin(uid: Int, totalValue: Double, daysAgo: Int = 3) {
        require(daysAgo in 1..7) { "phải nằm trong cửa sổ 7 ngày và không phải hôm nay" }
        withDb { db ->
            db.prepareStatement(
                "DELETE FROM logs.user_block_reward WHERE uid = ?"
            ).use { it.setInt(1, uid); it.executeUpdate() }

            if (totalValue == 0.0) return@withDb
            db.prepareStatement(
                """
                INSERT INTO logs.user_block_reward
                    (uid, reward_type, network, values_old, values_changed, values_new, reason, changed_at)
                VALUES (?, 'BCOIN', 'BSC', 0, ?, ?, 'Save game', CURRENT_DATE - ? * INTERVAL '1 day')
                """.trimIndent()
            ).use { st ->
                st.setInt(1, uid)
                st.setDouble(2, totalValue)
                st.setDouble(3, totalValue)
                st.setInt(4, daysAgo)
                st.executeUpdate()
            }
        }
    }

    fun clearPriceCache() {
        RedisClient.create(redisUrl).use { client ->
            client.connect().use { it.sync().del(AUTO_MINE_PRICE_KEY) }
        }
    }

    companion object {
        private val LOCAL_HOSTS = listOf("localhost", "127.0.0.1")
        private const val AUTO_MINE_PRICE_KEY = "TR_DATA:AUTO_MINE_PRICE"
    }
}
