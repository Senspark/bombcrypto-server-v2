package com.senspark.game.manager.rental

import com.senspark.common.service.IServerService

/**
 * Consumes the AP_RENTAL_SYNC stream published by the marketplace backend when a
 * P2P house rental changes (listed, rented, charged, ended).
 */
interface IHouseRentalResponseManager : IServerService {
    fun listenRentalSync(message: String): Boolean
}
