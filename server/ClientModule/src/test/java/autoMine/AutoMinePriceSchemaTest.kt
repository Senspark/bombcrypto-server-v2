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
 * Test tích hợp thật, chạy qua SmartFox đang chạy trên máy.
 *
 * Mục đích: chốt rằng JSON gửi về client cho AUTO_MINE_PRICE_V2/V3 **không đổi** sau khi sửa
 * tầng cache giá auto-mine. Cách dùng gồm hai lượt:
 *
 *   1. Chạy khi sfs-game-1 còn code CŨ  -> sinh ra file golden trong build/auto-mine-golden/
 *   2. Build code mới, chạy lại          -> đối chiếu với golden, khác là hỏng
 *
 * Lượt 1 chỉ làm được một lần: build code mới rồi thì mốc so sánh biến mất.
 *
 * Vì sao là BSC: nó có cấu hình trong config_package_auto_mine, và không phải airdrop user, nên
 * request đi vào nhánh packagePrice() — nhánh dùng cache và fn_calculate_package_auto_price.
 * Tài khoản airdrop (TON/SOL/RON/VIC/BAS) rẽ sang packagePriceUserAirdrop(), vốn trả thẳng
 * min_price và không chạm gì tới đoạn code đang cần kiểm tra: test sẽ xanh mà chẳng chứng minh
 * được gì. Guest cũng không dùng được — dataType TR không có dòng nào trong config, toJsonArray
 * sẽ ném NPE.
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
        // 1000 BCOIN trong cửa sổ 7 ngày: với price_percent = 12 thì giá thành 120, vượt hẳn
        // min_price = 10, nên bài test chạm được nhánh "đào nhiều trả nhiều" chứ không chỉ
        // nhánh giá sàn.
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

    /**
     * Gọi hai lần: lần đầu cache lạnh (chạy hẳn query), lần sau cache nóng. Hai lần phải giống
     * hệt nhau — đường cache không được làm méo dữ liệu.
     */
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
