package com.senspark.game.declare

import com.senspark.game.exception.CustomException

class EnumConstants {
    enum class BLOCK_REWARD_TYPE(val value: Int, val isTraditionalReward: Boolean) {
        BCOIN(1, false),
        BOMBERMAN(2, false),
        KEY(3, false),
        BCOIN_DEPOSITED(4, false),
        LUS(5, false),
        SENSPARK(7, false),
        SENSPARK_DEPOSITED(8, false),
        MSPc(9, false),
        WOFM(10, false),
        BOSS_TICKET(11, false),
        PVP_TICKET(12, false),
        NFT_PVP(13, false),
        GEM(14, true),
        GEM_LOCKED(16, true),
        GOLD(17, true),
        COIN(18, false),
        PLATINUM_CHEST(19, true),
        BRONZE_CHEST(20, true),
        SILVER_CHEST(21, true),
        GOLD_CHEST(22, true),
        ROCK(23, false),
        TON_DEPOSITED(24, false),
        SOL_DEPOSITED(25, false),
        RON_DEPOSITED(26, false),
        BAS_DEPOSITED(27, false),
        VIC_DEPOSITED(28, false);

        @Throws(CustomException::class)
        fun swapDepositedOrReward(): BLOCK_REWARD_TYPE {
            return when (this) {
                BCOIN -> BCOIN_DEPOSITED
                SENSPARK -> SENSPARK_DEPOSITED
                GEM -> GEM_LOCKED
                BCOIN_DEPOSITED -> BCOIN
                SENSPARK_DEPOSITED -> SENSPARK
                GEM_LOCKED -> GEM
                ROCK -> ROCK
                else -> throw CustomException(
                    String.format("type of %s can not swap", this.name),
                    ErrorCode.INVALID_PARAMETER
                )
            }
        }
        
        fun convertDepositToNetworkType(): DataType {
            return when (this) {
                TON_DEPOSITED -> DataType.TON
                SOL_DEPOSITED -> DataType.SOL
                RON_DEPOSITED -> DataType.RON
                BAS_DEPOSITED -> DataType.BAS
                VIC_DEPOSITED -> DataType.VIC
                else -> DataType.UNKNOWN
            }
        }

        fun getDataType(currentDataType: DataType): DataType {
            if (this.isTraditionalReward) {
                return DataType.TR
            }
            // Coin giờ đc xem như 1 tài nguyên TR, network nào cũng có và riêng user airdrop sẽ có thêm COIN network đó
            if(this == COIN){
                return DataType.TR
            }
            if (currentDataType.isEthereumAirdropUser()) {
                return currentDataType
            }
            return currentDataType
        }

        companion object {
            private val BY_VALUE: MutableMap<Int, BLOCK_REWARD_TYPE> = HashMap()

            init {
                for (e in entries) {
                    BY_VALUE[e.value] = e
                }
            }

            @Throws(CustomException::class)
            fun valueOf(value: Int): BLOCK_REWARD_TYPE {
                if (!BY_VALUE.containsKey(value)) {
                    throw CustomException(
                        String.format("Block reward %d not exists", value),
                        ErrorCode.INVALID_PARAMETER
                    )
                }
                return BY_VALUE[value]!!
            }

            @Throws(CustomException::class)
            fun fromItemId(value: Int): BLOCK_REWARD_TYPE {
                return when (value) {
                    102 -> GEM
                    103 -> GEM_LOCKED
                    0, 104 -> GOLD
                    105 -> COIN
                    else -> throw CustomException("Cannot convert item id %d to block reward")
                }
            }
        }
    }

    enum class MODE(val value: Int) {
        PVE(1),
        STORY(2),
        PVE_V2(3),
        PVP_WIN(4),
        PVP_CLAIM(5),
        BOSS_CLAIM(6),
        PVP(7),
        SWAP(8),
        ADVENTURE(9),
        GACHA(10),
        FREE(11),
        TH_MODE_V2(12);

        companion object {
            @Throws(CustomException::class)
            fun valueOf(value: Int): MODE {
                return when (value) {
                    1 -> PVE
                    2 -> STORY
                    3 -> PVE_V2
                    4 -> PVP_WIN
                    5 -> PVP_CLAIM
                    6 -> BOSS_CLAIM
                    7 -> PVP
                    else -> throw CustomException(String.format("Mode %s not valid", value), ErrorCode.SERVER_ERROR)
                }
            }
        }
    }

    enum class STORY_MODE_TICKET_TYPE(val value: Int) {
        PLAY_TO_EARN(0),
        PLAY_FOR_FUN(1),
        PLAY_TOURNAMENT(2)
    }

    enum class REFUND_REWARD_TYPE(val value: Int) {
        CONNECT_API_FAIL(1)
    }

    enum class SAVE {
        MAP,
        REWARD,
        HERO_STATUS,
        KEY,
    }

    enum class LoginType(val value: Int) {
        UNKNOWN(-1),
        BNB_POL(0),
        USERNAME_PASSWORD(1),
        MASTER(2),
        GUEST(3),
        FACEBOOK(4),
        APPLE(5),
        TON(6),
        SOL(7);

        companion object {
            fun from(value: Int?) = entries.firstOrNull {
                it.value == value
            } ?: UNKNOWN
        }
    }

    enum class DataType(val value: String) {
        UNKNOWN(""), // mới thêm
        BSC("BSC"),
        POLYGON("POLYGON"),
        TR("TR"),
        TON("TON"),
        SOL("SOL"),
        GUEST("GUEST"), // mới thêm
        RON("RON"),
        VIC("VIC"),
        BAS("BAS")
        ;

        fun isAirdropUser(): Boolean {
            return this == TON || this == SOL || this == RON || this == VIC || this == BAS
        }
        
        fun isEthereumAirdropUser(): Boolean{
            return this == RON || this == VIC || this == BAS
        }

        fun isEthereumUser(): Boolean{
            return this == BSC || this == POLYGON || this == RON || this == VIC || this == BAS
        }

        // hiện etherum có network RON và BASE dùng starcore nên để tránh conflict với starcore bsc/polygon
        // cần thêm netword type cho starcore của RON và BAS, mấy cái còn lại vẫn để TR như cũ
        fun getCoinType(isFi: Boolean = false): DataType{
            if(this.isEthereumAirdropUser() && isFi) {
                return this
            }
            return TR
        }
        
        fun convertToDepositType(): BLOCK_REWARD_TYPE {
            return when (this) {
                TON -> BLOCK_REWARD_TYPE.TON_DEPOSITED
                SOL -> BLOCK_REWARD_TYPE.SOL_DEPOSITED
                RON -> BLOCK_REWARD_TYPE.RON_DEPOSITED
                BAS -> BLOCK_REWARD_TYPE.BAS_DEPOSITED
                VIC -> BLOCK_REWARD_TYPE.VIC_DEPOSITED
                else -> throw CustomException("Unsupported token network: ${this.name}")
            }
        }

        companion object {
            fun from(value: String?) = entries.firstOrNull {
                it.value == value
            } ?: UNKNOWN
        }
    }

    enum class DepositType(val value: Int) {
        TON_DEPOSIT(0),
        SOL_DEPOSIT(1),
        BCOIN_DEPOSIT(2),
        RON_DEPOSIT(3),
        BAS_DEPOSIT(4),
        VIC_DEPOSIT(5);


        companion object {
            private val BY_VALUE: MutableMap<Int, DepositType> = HashMap()

            init {
                for (e in entries) {
                    BY_VALUE[e.value] = e
                }
            }

            @Throws(CustomException::class)
            fun valueOf(value: Int): DepositType {
                if (!BY_VALUE.containsKey(value)) {
                    throw CustomException(
                        String.format("BDeposit type %d not exists", value),
                        ErrorCode.INVALID_PARAMETER
                    )
                }
                return BY_VALUE[value]!!
            }
        }
    }

    enum class UserMode {
        TRIAL,
        NON_TRIAL
    }

    enum class UserType {
        FI,
        TR,
        GUEST,
        SOL;

        val isUserTraditional: Boolean
            get() = this == TR || this == GUEST
    }

    enum class HeroTRType {
        HERO,
        SOUL
    }

    enum class HeroType(val value: Int) {
        FI(0),
        TRIAL(1),
        TR(2),
        TON(3),
        SOL(4),
        RON(5),
        BAS(6),
        VIC(7);

        fun isAirdropHero(): Boolean {
            return this == TON 
                    || this == SOL 
                    || this == RON 
                    || this == VIC 
                    || this == BAS
        }

        companion object {
            private val BY_VALUE: MutableMap<Int, HeroType> = HashMap()

            init {
                for (e in entries) {
                    BY_VALUE[e.value] = e
                }
            }

            @Throws(CustomException::class)
            fun valueOf(value: Int): HeroType {
                if (!BY_VALUE.containsKey(value)) {
                    throw CustomException(String.format("Hero type %d not exists", value), ErrorCode.INVALID_PARAMETER)
                }
                return BY_VALUE[value]!!
            }
        }
    }

    enum class TokenType(val value: String) {
        BCOIN("BCOIN"),
        COIN("COIN")
    }

    enum class StakeVipRewardType(val value: String) {
        REWARD("REWARD"),
        BOOSTER("BOOSTER")
    }

    enum class DeviceType(val value: String) {
        UNKNOWN(""),
        WEB("WEB"),
        MOBILE("MOBILE"),
        ;

        companion object {
            fun from(value: String?) = entries.firstOrNull {
                it.value == value
            } ?: UNKNOWN
        }
    }

    enum class MatchResult {
        WIN,
        LOSE,
        OUT
    }

    enum class Platform(val value: Int) {
        Unknown(0),
        WebPC(1),
        WebTelegram(2),
        WebOther(3),
        MobileTelegram(4),
        AndroidNative(5),
        IOSNative(6),
        Editor(7),
        IosTelegram(8),
        AndroidTelegram(9),
        ;

        companion object {
            fun from(value: Int?) = entries.firstOrNull {
                it.value == value
            } ?: Unknown
        }
    }

    enum class ZoneName {
        ServerMain,  // user online ở server Web + Mobile
        ServerPvp,  // user online ở server pvp
        ThMode,  // user đang chơi th mode
    }

    enum class ClubType {
        TELEGRAM,
        TON,
    }
}
