package com.senspark.game.manager.treasureHuntV2

import com.senspark.common.cache.IMessengerService
import com.senspark.game.constant.ChannelKeys
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.utils.JsonExtensionBuilder
import com.senspark.game.utils.ManualScheduler
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** [THModeRaceBroadcaster]: gộp trạng thái race theo hero, mỗi giây một PUBLISH. */
class THModeRaceBroadcasterTest {

    private lateinit var messenger: IMessengerService
    private lateinit var scheduler: ManualScheduler
    private lateinit var sut: THModeRaceBroadcaster
    private val published = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        messenger = mockk(relaxed = true)
        every { messenger.publish(ChannelKeys.SV_TH_MODE_RACE_CHANNEL, capture(published)) } just Runs
        scheduler = ManualScheduler()
        sut = THModeRaceBroadcaster(messenger, scheduler, mockk(relaxed = true))
        sut.start()
    }

    private fun hero(id: Int, dataType: DataType = DataType.BSC): Hero {
        val hero = mockk<Hero>(relaxed = true)
        every { hero.heroId } returns id
        every { hero.isHeroS } returns false
        every { hero.isFakeS } returns false
        every { hero.details.dataType } returns dataType
        every { hero.stakeBcoin } returns 100.0
        every { hero.stakeSen } returns 0.0
        return hero
    }

    private fun tick() = scheduler.step(1000)

    private fun batch(index: Int): List<THModeRaceBroadcaster.DataThModeRedis> =
        JsonExtensionBuilder.json.decodeFromString(published[index])

    @Test
    fun `many records of one hero become one entry with the latest ticket count`() {
        val h = hero(1)
        repeat(50) { i -> sut.record(7, 1, "user", h, i + 1, 0) }

        verify(exactly = 0) { messenger.publish(any(), any()) }
        tick()

        assertEquals(1, published.size)
        val entries = batch(0)
        assertEquals(1, entries.size)
        assertEquals(50, entries[0].ticketCount)
    }

    @Test
    fun `a tick with nothing recorded publishes nothing`() {
        tick()

        verify(exactly = 0) { messenger.publish(any(), any()) }
    }

    @Test
    fun `entries of an older race come first`() {
        sut.record(6, 1, "user", hero(1), 1, 0)
        sut.record(5, 2, "user", hero(2), 1, 0)
        tick()

        assertEquals(listOf(5, 6), batch(0).map { it.raceId })
    }

    @Test
    fun `the same hero id on two networks stays two entries`() {
        sut.record(7, 1, "user", hero(1, DataType.BSC), 1, 0)
        sut.record(7, 1, "user", hero(1, DataType.POLYGON), 1, 0)
        tick()

        assertEquals(2, batch(0).size)
    }

    @Test
    fun `a failing publish does not stop later ticks`() {
        var calls = 0
        every { messenger.publish(any(), any()) } answers {
            calls++
            if (calls == 1) throw RuntimeException("redis down")
        }

        sut.record(7, 1, "user", hero(1), 1, 0)
        tick()
        sut.record(7, 1, "user", hero(1), 2, 0)
        tick()

        assertEquals(2, calls)
    }
}
