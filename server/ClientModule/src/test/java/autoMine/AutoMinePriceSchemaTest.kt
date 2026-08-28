package autoMine

import com.senspark.game.declare.SFSCommand
import com.senspark.testclient.EditorLogin
import com.senspark.testclient.SfsTestClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test tích hợp qua SmartFox chạy trên máy: chốt JSON trả về cho AUTO_MINE_PRICE_V2/V3 không đổi.
 * Lượt chạy đầu sinh file golden trong build/auto-mine-golden/, các lượt sau đối chiếu với nó.
 *
 * Dùng BSC vì chỉ nhánh này đi qua packagePrice() — cache và fn_calculate_package_auto_price.
 * Tài khoản airdrop rẽ sang packagePriceUserAirdrop() nên không chạm code cần kiểm tra.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutoMinePriceSchemaTest {

    private val walletAddress = "editor_automine_test"
    private val fixture = TestFixture()
    private val client = SfsTestClient()
    private val goldenDir = File("build/auto-mine-golden")

    @BeforeAll
    fun setup() {
        goldenDir.mkdirs()
        client.login(walletAddress, EditorLogin())

        val uid = fixture.findUid(walletAddress)
            ?: error("không tìm thấy user $walletAddress sau khi đăng nhập")
        // Đủ lớn để giá vượt min_price, chạm nhánh tính theo sản lượng chứ không phải giá sàn.
        fixture.seedMinedBcoin(uid, totalValue = 1000.0)
        fixture.clearPriceCache()
    }

    @AfterAll
    fun teardown() {
        client.close()
    }

    @Test
    fun `V2 response schema is unchanged`() = checkSchema(SFSCommand.AUTO_MINE_PRICE_V2)

    @Test
    fun `V3 response schema is unchanged`() = checkSchema(SFSCommand.AUTO_MINE_PRICE_V3)

    /** Cache lạnh rồi cache nóng: hai lần phải giống hệt nhau. */
    private fun checkSchema(command: String) {
        fixture.clearPriceCache()
        val cold = client.send(command)
        val warm = client.send(command)

        assertEquals(cold, warm, "$command: cache nóng khác cache lạnh")
        assertTrue(cold.contains("packages"), "$command: thiếu trường packages -> $cold")

        val golden = File(goldenDir, "$command.json")
        if (!golden.exists()) {
            golden.writeText(cold)
            println("[golden] đã ghi ${golden.absolutePath}")
            println("[golden] $cold")
            return
        }

        assertEquals(
            golden.readText(),
            cold,
            "$command: response khác golden. Nếu khác biệt là có chủ đích thì xoá " +
                "${golden.absolutePath} rồi chạy lại để chụp mốc mới."
        )
    }
}
