package com.senspark.game.constant

/**
 * Dùng cho CacheService
 * Phân biệt keys cho ko bị trùng
 */
class CachedKeys {
    companion object {
        const val IAP_GEM_PURCHASED = "TR_DATA:IAP_GEM_PURCHASED"
        const val IAP_SPECIAL_OFFER_BOUGHT = "TR_DATA:IAP_SPECIAL_OFFER_BOUGHT"
        const val IAP_USER_IAP_PACK_END = "TR_DATA:IAP_USER_IAP_PACK_END"
        const val USER_OLD_ITEM = "TR_DATA:USER_OLD_ITEM"
        const val COSTUME_PRESET = "TR_DATA:COSTUME_PRESET"
        const val USER_MISSION = "TR_DATA:USER_MISSION"
        const val USER_CONFIG = "USER_CONFIG:"
        const val AUTO_MINE_PRICE = "TR_DATA:AUTO_MINE_PRICE"
        const val TH_RACE = "TH_RACE:"
        const val TH_CUR_RACE = "TH_CUR_RACE"
        const val TH_CHEAT_HERO = "TH_CHEAT_HERO:"
        const val TH_CHEAT_TH_MODE = "TH_CHEAT_TH_MODE:"
        const val PRIVATE_KEY = "PR_KEY:"
        const val SV_USR_HASH = "SV_USR_HASH"
        const val SV_SERVER_PVP_INFO = "SV_SERVER_PVP_INFO"
        const val AES_KEY = "SV:LOGIN:AES_KEY"
        
        const val SV_TOURNAMENT_WHITELIST = "SV_TOURNAMENT_WHITELIST"
        const val SV_HANDLER_LOGGER = "SV_HANDLER_LOGGER"
        
        const val SV_TODAY_TASK = "SV_TODAY_TASK"

        const val USER_ONLINE = "USER_ONLINE"

        const val MARKET_MIN_PRICE = "MARKET:MIN_PRICE"
        
        const val BLACK_LIST_SWAP_GEM = "SWAP_GEM:BLACK_LIST"
        const val WHITE_LIST_SWAP_GEM = "SWAP_GEM:WHITE_LIST"
        const val CHEAT_SWAP_GEM_UID = "CHEAT_SWAP_GEM_UID"
        const val SV_CURRENT_USER_SEND_LOG = "SV_CURRENT_USER_SEND_LOG"
    }
}

/**
 * Tên được đặt theo nguồn phát event này:
 * - AP: Api backend
 * - SV: Server Smartfox
 */
class StreamKeys {
    companion object {
        const val AP_BL_HERO_STAKE_STR = "AP_BL_HERO_STAKE_STR"

        const val SV_GAME_JOIN_PVP_STR = "SV_GAME_JOIN_PVP_STR" // server game -> api pvp-matching
        const val SV_GAME_LEAVE_PVP_STR = "SV_GAME_LEAVE_PVP_STR" // server game -> api pvp-matching

        const val AP_PVP_MATCH_FOUND_STR = "AP_PVP_MATCH_FOUND_STR" // api pvp-matching -> server game

        const val SV_PVP_MATCH_STARTED_STR = "SV_PVP_MATCH_STARTED_STR"
        const val SV_PVP_MATCH_UPDATED_STR = "SV_PVP_MATCH_UPDATED_STR" // server pvp -> api analytic
        const val SV_PVP_MATCH_FINISHED_STR = "SV_PVP_MATCH_FINISHED_STR" // server pvp -> server game, api pvp-matching
        const val AP_TON_TRANSACTION = "AP:TON:MERCHANT:TRANSACTION"  //api deposit -> server game.
        const val AP_CREATE_CLUB = "AP_CREATE_CLUB"  //api telegram bot -> server game
        const val AP_JOIN_CLUB = "AP_JOIN_CLUB"  //api telegram bot -> server game
        const val AP_LEAVE_CLUB = "AP_LEAVE_CLUB"  //api telegram bot -> server game

        const val AP_SOL_TRANSACTION = "AP:SOL:MERCHANT:TRANSACTION"
        const val AP_RON_TRANSACTION = "AP:RON:MERCHANT:TRANSACTION"//api deposit ron -> server game.
        const val AP_BAS_TRANSACTION = "AP:BAS:MERCHANT:TRANSACTION"//api deposit bas -> server game.
        const val AP_VIC_TRANSACTION = "AP:VIC:MERCHANT:TRANSACTION"//api deposit vic -> server game.

        const val SV_TH_MODE_RACE = "SV_TH_MODE_RACE"
        const val SV_KICK_USER = "SV_KICK_USER"
        const val SV_ADMIN_COMMAND = "SV:ADMIN_COMMAND"
        
        const val AP_MONETIZATION_ADS_VERIFY =  "AP_MONETIZATION:ADS_VERIFY"
        
        const val AP_BL_SYNC_HERO = "AP_BL_SYNC_HERO"
        const val AP_BL_SYNC_HOUSE = "AP_BL_SYNC_HOUSE"
        const val AP_BL_SYNC_DEPOSIT = "AP_BL_SYNC_DEPOSIT"
    }
}