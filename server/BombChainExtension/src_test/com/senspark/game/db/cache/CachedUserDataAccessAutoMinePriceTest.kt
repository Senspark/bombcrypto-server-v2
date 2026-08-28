package com.senspark.game.db.cache

import com.google.gson.JsonArray
import com.senspark.common.cache.ICacheService
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.CachedKeys
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Cache giá auto-mine trong [CachedUserDataAccess]: single-flight, cache key, xử lý lỗi. */
class CachedUserDataAccessAutoMinePriceTest {

    private val uid = 321366
    private val dataType = DataType.BSC
    private val expectedField = "${uid}_BSC"

    private lateinit var bridge: IUserDataAccess
    private lateinit var cache: ICacheService
    private lateinit var logger: ILogger
    private lateinit var sut: CachedUserDataAccess
    private val pool = Executors.newFixedThreadPool(16)

    @BeforeTest
    fun setup() {
        bridge = mockk(relaxed = true)
        cache = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        sut = CachedUserDataAccess(bridge, cache, logger)

        // Mặc định: cache rỗng, DB trả về một package hợp lệ.
        every { cache.getFromHash(CachedKeys.AUTO_MINE_PRICE, any()) } returns null
        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } answers { pricePayload() }
    }

    @AfterTest
    fun teardown() {
        pool.shutdownNow()
    }

    /** Hình dạng fn_calculate_package_auto_price trả về. */
    private fun pricePayload(price: Double = 120.0): ISFSArray {
        val pkg = SFSObject()
        pkg.putUtfString("package", "PACKAGE_7_DAYS")
        pkg.putInt("num_days", 7)
        pkg.putDouble("price_percent", 10.0)
        pkg.putDouble("min_price", 100.0)
        pkg.putDouble("price", price)
        return SFSArray().apply { addSFSObject(pkg) }
    }

    private fun call(): ISFSArray = sut.loadAutoMinePackagePrice(uid, dataType, JsonArray())

    /** Nhiều request cùng một user, cache lạnh, phải gộp thành đúng một lần chạm database. */
    @Test
    fun `concurrent calls for one uid hit the database once`() {
        val threads = 16
        val hits = AtomicInteger()
        val ready = CountDownLatch(threads)
        val go = CountDownLatch(1)
        val done = CountDownLatch(threads)

        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } answers {
            hits.incrementAndGet()
            // Giữ chậm để mọi luồng kịp dồn vào cùng lúc.
            Thread.sleep(200)
            pricePayload()
        }

        repeat(threads) {
            pool.submit {
                ready.countDown()
                go.await()
                try {
                    call()
                } finally {
                    done.countDown()
                }
            }
        }
        ready.await(5, TimeUnit.SECONDS)
        go.countDown()
        assertTrue(done.await(20, TimeUnit.SECONDS), "các luồng phải kết thúc, không được treo")

        assertEquals(1, hits.get(), "$threads request đồng thời chỉ được chạm DB một lần")
        verify(exactly = 1) { bridge.loadAutoMinePackagePrice(uid, dataType, any()) }
    }

    /** Hai uid khác nhau không được chặn nhau — khoá là theo key, không phải toàn cục. */
    @Test
    fun `different uids do not block each other`() {
        val other = 3215
        sut.loadAutoMinePackagePrice(uid, dataType, JsonArray())
        sut.loadAutoMinePackagePrice(other, dataType, JsonArray())

        verify(exactly = 1) { bridge.loadAutoMinePackagePrice(uid, dataType, any()) }
        verify(exactly = 1) { bridge.loadAutoMinePackagePrice(other, dataType, any()) }
    }

    /**
     * addPrices() sửa thẳng vào object nhận được, nên dùng chung một instance sẽ làm luồng thứ
     * hai mất mảng "prices". Mỗi luồng phải nhận bản riêng.
     */
    @Test
    fun `each caller gets its own copy, mutating one leaves the other intact`() {
        val first = call()

        val writtenJson = slot<String>()
        verify { cache.setToHash(CachedKeys.AUTO_MINE_PRICE, expectedField, capture(writtenJson), any()) }
        every { cache.getFromHash(CachedKeys.AUTO_MINE_PRICE, expectedField) } returns writtenJson.captured
        val second = call()

        assertTrue(first !== second, "hai lần gọi không được trả về cùng một instance")

        val pkg = first.getSFSObject(0)
        pkg.putSFSArray("prices", SFSArray())
        pkg.removeElement("price")

        assertNull(first.getSFSObject(0).getDouble("price"), "bản thứ nhất đã bị sửa đúng như addPrices làm")
        assertEquals(
            120.0,
            second.getSFSObject(0).getDouble("price"),
            "bản thứ hai phải còn nguyên price, nếu không package về client sẽ mất mảng prices"
        )
    }

    /** JSON lúc cache miss và lúc hit phải trùng khít. */
    @Test
    fun `cache hit and cache miss return identical json`() {
        val fromDb = call()

        val writtenJson = slot<String>()
        verify { cache.setToHash(CachedKeys.AUTO_MINE_PRICE, expectedField, capture(writtenJson), any()) }
        every { cache.getFromHash(CachedKeys.AUTO_MINE_PRICE, expectedField) } returns writtenJson.captured

        val fromCache = call()
        assertEquals(fromDb.toJson(), fromCache.toJson())
    }

    /** Luồng chờ cũng phải nhận bản riêng, không phải instance của luồng chạy query. */
    @Test
    fun `a waiting thread gets its own copy, not the leader's instance`() {
        val leaderStarted = CountDownLatch(1)
        val releaseLeader = CountDownLatch(1)
        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } answers {
            leaderStarted.countDown()
            releaseLeader.await(5, TimeUnit.SECONDS)
            pricePayload()
        }

        val leaderResult = pool.submit<ISFSArray> { call() }
        assertTrue(leaderStarted.await(5, TimeUnit.SECONDS))
        val followerResult = pool.submit<ISFSArray> { call() }
        Thread.sleep(100) // đủ để follower kịp vào trạng thái chờ
        releaseLeader.countDown()

        val a = leaderResult.get(10, TimeUnit.SECONDS)
        val b = followerResult.get(10, TimeUnit.SECONDS)
        assertTrue(a !== b, "luồng chờ không được dùng chung instance với luồng chạy query")
        assertEquals(a.toJson(), b.toJson(), "nội dung vẫn phải giống nhau")
    }

    /** Key không còn đóng dấu ngày, nên cả cache không cùng miss lúc nửa đêm. */
    @Test
    fun `cache key combines uid and dataType with no day stamp`() {
        val field = slot<String>()
        call()
        verify { cache.setToHash(CachedKeys.AUTO_MINE_PRICE, capture(field), any(), any()) }

        assertEquals(expectedField, field.captured)
        val today = java.time.LocalDate.now().dayOfMonth.toString()
        assertTrue(
            !field.captured.endsWith("_$today"),
            "key không được đóng dấu ngày, nếu không cả cache lại cùng miss lúc nửa đêm"
        )
    }

    /** Hai network của cùng một user không được dùng chung một entry giá. */
    @Test
    fun `different dataTypes get separate cache keys`() {
        val fields = mutableListOf<String>()
        every { cache.setToHash(CachedKeys.AUTO_MINE_PRICE, capture(fields), any(), any()) } returns Unit

        sut.loadAutoMinePackagePrice(uid, DataType.BSC, JsonArray())
        sut.loadAutoMinePackagePrice(uid, DataType.POLYGON, JsonArray())

        assertEquals(listOf("${uid}_BSC", "${uid}_POLYGON"), fields)
    }

    /** Redis hỏng không được làm hỏng request: database đã tính ra giá hợp lệ rồi. */
    @Test
    fun `a redis failure does not fail the request`() {
        every { cache.getFromHash(any(), any()) } throws RuntimeException("redis down")
        every { cache.setToHash(any(), any(), any(), any()) } throws RuntimeException("redis down")

        val result = call()

        assertNotNull(result)
        assertEquals(120.0, result.getSFSObject(0).getDouble("price"))
        verify(exactly = 1) { bridge.loadAutoMinePackagePrice(uid, dataType, any()) }
    }

    /** Query lỗi thì luồng chờ phải nhận đúng lỗi đó, không treo tới hết timeout. */
    @Test
    fun `when the query fails the waiting thread sees the same error`() {
        val leaderStarted = CountDownLatch(1)
        val releaseLeader = CountDownLatch(1)
        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } answers {
            leaderStarted.countDown()
            releaseLeader.await(5, TimeUnit.SECONDS)
            throw IllegalStateException("db exploded")
        }

        val leader = pool.submit { call() }
        assertTrue(leaderStarted.await(5, TimeUnit.SECONDS))
        val follower = pool.submit { call() }
        Thread.sleep(100)
        releaseLeader.countDown()

        val leaderError = assertFailsWith<java.util.concurrent.ExecutionException> {
            leader.get(10, TimeUnit.SECONDS)
        }
        val followerError = assertFailsWith<java.util.concurrent.ExecutionException> {
            follower.get(10, TimeUnit.SECONDS)
        }
        assertEquals("db exploded", leaderError.cause?.message)
        assertEquals("db exploded", followerError.cause?.message, "luồng chờ phải thấy lỗi thật, không phải timeout")
    }

    /** Sau khi một lượt kết thúc, chỗ đang-chạy phải được nhả để lượt sau còn chạy được. */
    @Test
    fun `the in-flight slot is released after a failure`() {
        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } throws IllegalStateException("boom")
        assertFailsWith<IllegalStateException> { call() }

        every { bridge.loadAutoMinePackagePrice(any(), any(), any()) } answers { pricePayload() }
        val result = call()

        assertEquals(120.0, result.getSFSObject(0).getDouble("price"))
    }
}
