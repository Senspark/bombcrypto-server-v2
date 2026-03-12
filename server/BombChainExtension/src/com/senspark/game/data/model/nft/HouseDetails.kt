package com.senspark.game.data.model.nft

import com.senspark.game.utils.DetailsExtractor

class HouseDetails(val details: String) {
    private val _extractor = DetailsExtractor(details)
    val houseId = _extractor.extract(0, 30)
    val index = _extractor.extract(30, 10)
    val rarity = _extractor.extract(40, 5)
    val recovery = _extractor.extract(45, 15)
    val capacity = _extractor.extract(60, 5)

    companion object {
        fun genHouseDetail(houseInt: Int, index: Int, rarity: Int, recovery: Int, capacity: Int): String {
            var mask = (0L).toBigInteger()
            val houseIdBit = houseInt.toBigInteger()
            mask = mask or houseIdBit
            val indexBit = index.toBigInteger() shl 30
            mask = mask or indexBit
            val rarityBit = rarity.toBigInteger() shl 40
            mask = mask or rarityBit
            val recoveryBit = recovery.toBigInteger() shl 45
            mask = mask or recoveryBit
            val capacityBit = capacity.toBigInteger() shl 60
            mask = mask or capacityBit
            return mask.toString()
        }
    }
}