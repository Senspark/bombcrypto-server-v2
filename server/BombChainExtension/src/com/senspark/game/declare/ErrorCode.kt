package com.senspark.game.declare

object ErrorCode {
    const val SUCCESS: Int = 0
    const val SYNC_USER_DATA_FAILED: Int = 75
    const val NOT_FOUND: Int = 404

    const val USER_IS_BANED: Int = 102

    const val SERVER_ERROR: Int = 1000
    const val CREATE_MAP_FAIL: Int = 1001
    const val BOMBERMAN_NULL: Int = 1002
    const val BOMBERMAN_OUT_OF_ENERGY: Int = 1003
    const val STARTEXPLODE_CAN_NOT_SET_BOOM: Int = 1004
    const val BOMBERMAN_IS_NOT_WORKING: Int = 1005
    const val BOMBERMAN_IS_WORKING: Int = 1006
    const val HOUSE_NOT_EXIST: Int = 1007
    const val HOUSE_LIMIT_REACH: Int = 1008
    const val HOUSE_IS_ACTIVATE: Int = 1009
    const val BOMBERMAN_ACTIVE_INVALID: Int = 1010
    const val BOMBERMAN_MAX_ACTIVE: Int = 1011
    const val NOT_ENOUGH_RESOURCE: Int = 1017
    const val STORY_MAP_NULL: Int = 1018
    const val NOT_ENOUGH_REWARD: Int = 1019
    const val USERNAME_EXIST: Int = 1020
    const val USER_REPORT_INVEST_FAIL: Int = 1022
    const val BOMBER_UPDATE_NAME_FAIL: Int = 1023
    const val PERMISSION_DENIED: Int = 1024
    const val CLAIM_FAILED: Int = 1025
    const val USERNAME_INVALID: Int = 1026
    const val PASSWORD_INVALID: Int = 1027
    const val INVALID_PARAMETER: Int = 1030

    const val NOT_DATA_PVP_RANKING: Int = 1031
    const val NOT_FOUND_PAGE_PVP_RANKING: Int = 1032
    const val NOT_CLAIM_PVP_REWARD: Int = 1033
    const val NOT_PVP_REWARD: Int = 1034
    const val WAS_USED_PVP_BOOSTER: Int = 1035
    const val STORY_HUNTER_LEVEL_INVALID: Int = 1036
    const val STORY_HUNTER_SEASON_INVALID: Int = 1037
    const val STORY_HUNTER_CLAIM_INVALID: Int = 1038
    const val NOT_ENOUGH_PVP_BOOSTER: Int = 1039
    const val CAN_NOT_MATCHING: Int = 1040
    const val PVP_SEASON_INVALID: Int = 1041
    const val PVE_WAS_NOT_STARTED: Int = 1042
    const val HACK_SPEED: Int = 1043
    const val HACK_EXPLODE_BLOCK: Int = 1044
    const val NOT_EXECUTE: Int = 1045

    const val OPEN_NFT_CHEST_FAIL: Int = 1042
    const val RELOAD_MARKETPLACE: Int = 1043
    const val COMPLETE_TRIAL: Int = 1044
    const val NOT_RECEIVE_HERO_TR: Int = 1045
    const val IAP_SHOP_BILL_ALREADY_USED: Int = 1046
    const val PVP_INVALID_MATCH_INFO: Int = 1047
    const val PVP_MATCH_EXPIRED: Int = 1048
    const val PVP_ALREADY_IN_QUEUE: Int = 1049
    const val PVP_NOT_IN_QUEUE: Int = 1050
    const val BOMBER_NOT_LEGACY: Int = 1051
    const val HERO_FI_IS_LOCKED: Int = 1052
    const val NOT_SUPPORT_EXPLODE_V2: Int = 1053
    const val NOT_SUPPORT_EXPLODE_V3: Int = 1054
    const val NOT_USER_TON: Int = 1055
    const val NOT_USER_SOLANA: Int = 1056
    const val NOT_USER_RON: Int = 1101
    const val NOT_USER_AIRDROP: Int = 1058
    const val NOT_SUPPORT_AIRDROP_USER: Int = 1058
    const val CLAIM_DAILY_TASK_FAIL: Int = 1059
    const val NOT_SUPPORTED: Int = 1060
    const val NOT_USER_BAS: Int = 1061
    const val NOT_USER_VIC: Int = 1062

    const val REQUIRE_PASSCODE: Int = 9999
}