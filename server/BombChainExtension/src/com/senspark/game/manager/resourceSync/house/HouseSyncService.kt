package com.senspark.game.manager.resourceSync.house


import com.senspark.game.controller.IUserController
import com.senspark.game.controller.IUserHouseManager
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.nft.HouseDetails
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class HouseSyncService(
    private val dataAccessManager: IDataAccessManager,
    private val gameDataAccess: IGameDataAccess,
) : IHouseSyncService {
    override fun syncHouses(userController: IUserController, syncedDetails: List<HouseDetails>): ISFSObject {
        val houseController: IUserHouseManager = userController.masterUserManager.houseManager
        val houseIds: MutableList<Int> = ArrayList()
        val houses: List<House> = houseController.toArray()
        for (house in houses) {
            var has = false
            for (details in syncedDetails) {
                if (house.houseId == details.houseId) {
                    has = true
                    break
                }
            }
            if (!has) {
                houseIds.add(house.houseId)
            }
        }

        // Check house nao chua co tren db add vao, hoặc có con upgrade thì thêm vào update
        val newDetails: MutableList<HouseDetails> = ArrayList()
        for (details in syncedDetails) {
            val house: House? = houseController.getHouse(details.houseId)
            if (house == null) {
                newDetails.add(details)
            }
        }

        // Kiểm tra nhà active mà đổi nhà nếu nhà active cũ bị xoá
        val currentActiveHouse: House? = houseController.activeHouse

        // Nếu nhà đang active nằm trong danh sách nhà bị xoá thì cho rest toàn bộ hero trong nhà
        if (currentActiveHouse != null && houseIds.any { it == currentActiveHouse.houseId }) {
            val bombermanRest: List<Hero> = userController.masterUserManager.heroFiManager.housingHeroes.map {
                userController.masterUserManager.heroFiManager.setSleep(it)
                it
            }

            // Cập nhật vào db
            dataAccessManager.gameDataAccess.updateBomberEnergyAndStage(
                userController.userId,
                userController.dataType,
                bombermanRest
            )
        }

        // Làm thôi
        if (houseIds.isNotEmpty()) {
            removeHouses(userController, houseIds)
        }
        addHouses(userController, newDetails)

        return getResponseSendToClient(userController, newDetails)
    }

    // Dùng để gọi sync house khi user offline, chỉ dùng đc cho user ko phải airdrop
    override fun syncHousesOffline(
        uid: Int,
        dataType: EnumConstants.DataType,
        syncedDetails: List<HouseDetails>,
        heroBuilder: IHeroBuilder
    ) {
        if (dataType.isAirdropUser()) {
            throw CustomException("Cannot sync house offline for airdrop user")
        }

        val houseNeedDelete: MutableList<Int> = ArrayList()
        val newHouseDetails: MutableList<HouseDetails> = ArrayList()

        // Để sync house offline
        //B1: Load toàn bộ house của user này trong db lên
        val houseInDb = gameDataAccess.loadUserHouse(dataType, uid, 1000000, 0)

        //B2: Kiểm tra và xoá các house ko còn tồn tại trong dữ liệu từ blockchain
        for (house in houseInDb) {
            var has = false
            for (details in syncedDetails) {
                if (house.value.houseId == details.houseId) {
                    has = true
                    break
                }
            }
            if (!has) {
                houseNeedDelete.add(house.value.houseId)
            }
        }

        //B3: Kiểm tra house nào chưa có trong db thì thêm vào newHouseDetails
        for (details in syncedDetails) {
            val house: House? = houseInDb.get(details.houseId)
            if (house == null) {
                newHouseDetails.add(details)
            }
        }

        //B4: Kiểm tra nhà bị xoá có phải là nhà đang active ko, nếu phải thì set sleep all hero trong đó
        val currentActiveHouse: House? = houseInDb.values.firstOrNull { it.isActive }
        if (currentActiveHouse != null && houseNeedDelete.any { it == currentActiveHouse.houseId }) {
            // Load tất cả hero của user này trong nhà để set trạng thái sang sleep
            val allHero = heroBuilder.getFiHeroes(uid, dataType, 1000000, 0).values.toList()
            val bombermanRest = allHero.filter { it.stage == GameConstants.BOMBER_STAGE.HOUSE }
            bombermanRest.forEach {
                it.stage = GameConstants.BOMBER_STAGE.SLEEP
                it.timeRest = System.currentTimeMillis()
            }

            // Cập nhật vào db
            dataAccessManager.gameDataAccess.updateBomberEnergyAndStage(
                uid,
                dataType,
                bombermanRest
            )
        }

        //B5: Update nhà đã bị xoá trên blockchain vô db
        if (houseNeedDelete.isNotEmpty()) {
            dataAccessManager.gameDataAccess.deleteHouseNotExist(dataType, uid, houseNeedDelete)
        }

        //B6: Thêm nhà mới vào db
        for (details in newHouseDetails) {
            val house = House.newInstance(details)
            dataAccessManager.gameDataAccess.insertNewHouse(dataType, uid, house)
        }
    }

    private fun removeHouses(userController: IUserController, houseIds: List<Int>) {
        if (houseIds.isEmpty()) {
            return
        }
        val houseController: IUserHouseManager = userController.masterUserManager.houseManager
        // Remove tren server
        for (id in houseIds) {
            houseController.removeHouse(id)
        }

        // Delete house tren DB
        dataAccessManager.gameDataAccess.deleteHouseNotExist(userController.dataType, userController.userId, houseIds)
    }

    private fun addHouses(userController: IUserController, detailList: List<HouseDetails>) {
        val houseController: IUserHouseManager = userController.masterUserManager.houseManager
        for (details in detailList) {
            val house = House.newInstance(details)
            dataAccessManager.gameDataAccess.insertNewHouse(userController.dataType, userController.userId, house)
            houseController.addHouse(house)
        }
    }

    private fun getResponseSendToClient(userController: IUserController, newDetails: List<HouseDetails>): ISFSObject {
        val data = SFSObject()
        val sfsHouses = SFSArray()
        val houseController: IUserHouseManager = userController.masterUserManager.houseManager
        val houses: List<House> = houseController.toArray()
        for (house in houses) {
            val objData = SFSObject()
            objData.putUtfString(SFSField.House_Gen_Id, house.details.details)
            objData.putInt(SFSField.Active, if (house.isActive) 1 else 0)
            sfsHouses.addSFSObject(objData)
        }
        val sfsArrNewHouse = SFSArray()
        for (details in newDetails) {
            sfsArrNewHouse.addLong(details.houseId.toLong())
        }
        data.putSFSArray(SFSField.Houses, sfsHouses)
        data.putSFSArray(SFSField.NewHouses, sfsArrNewHouse)

        return data
    }
}