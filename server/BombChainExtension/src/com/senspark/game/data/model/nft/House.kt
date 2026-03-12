package com.senspark.game.data.model.nft

import java.sql.ResultSet

class House(
    val details: HouseDetails,
    private var _active: Boolean,
    var endTimeRent: Long
) {
    companion object {
        @JvmStatic
        fun fromResultSet(rs: ResultSet): House {
            val details = HouseDetails(rs.getString("gen_house_id"))
            return House(
                details,
                rs.getInt("active") == 1,
                rs.getLong("end_time_rent_convert"),
            )
        }

        @JvmStatic
        fun oldHouseFromResultSet(rs: ResultSet): House {
            val details = HouseDetails(rs.getString("gen_house_id"))
            return House(
                details,
                rs.getInt("active") == 1,
                0L,
            )
        }

        @JvmStatic
        fun newInstance(details: HouseDetails): House {
            return House(details, false, 0L)
        }
    }

    val houseId = details.houseId
    val rarity = details.rarity
    val recovery = details.recovery / 60
    val capacity = details.capacity

    var isActive
        get() = _active
        set(value) {
            _active = value
        }
}