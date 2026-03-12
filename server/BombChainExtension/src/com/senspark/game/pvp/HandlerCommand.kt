package com.senspark.game.pvp

object HandlerCommand {
    /** When a match is found for the current user. */
    const val FoundMatch = "PVP_FOUND_MATCH"

    /** When a match is started. */
    const val StartMatch = "PVP_START_MATCH"

    /** When a match is finished. */
    const val FinishMatch = "PVP_FINISH_MATCH"

    const val StartReady = "PVP_START_READY"
    const val FinishReady = "PVP_FINISH_READY"
    const val StartRound = "PVP_START_ROUND"
    const val FinishRound = "PVP_FINISH_ROUND"

    /**
     * Utility commands.
     */
    const val PingPong = "PVP_PING_PONG"

    /**
     * Participant commands.
     */
    const val Ready = "PVP_READY"
    const val Quit = "PVP_QUIT"
    const val MoveHero = "PVP_MOVE_HERO"
    const val PlantBomb = "PVP_PLANT_BOMB"
    const val ThrowBomb = "PVP_THROW_BOMB"
    const val UseBooster = "PVP_USE_BOOSTER"
    const val UseEmoji = "PVP_USE_EMOJI"

    /**
     * Observer commands.
     */
    const val ObserverReady = "PVP_OBSERVER_READY"
    const val ObserverUseEmoji = "PVP_OBSERVER_USE_EMOJI"
    const val ObserverChangeState = "PVP_OBSERVER_CHANGE_STATE"
    const val ObserverFallingBlock = "PVP_OBSERVER_FALLING_BLOCK"
    
    const val DepositTonResponse = "DEPOSIT_TON_RESPONSE"
    const val DepositSolResponse = "DEPOSIT_SOL_RESPONSE"
    const val DepositRonResponse = "DEPOSIT_RON_RESPONSE"
    const val DepositBasResponse = "DEPOSIT_BAS_RESPONSE"
    const val DepositVicResponse = "DEPOSIT_VIC_RESPONSE"
    
    const val VerifyAdsResponse = "VERIFY_ADS_RESPONSE"
    
    const val ForceClientSendLog = "FORCE_CLIENT_SEND_LOG"
    
    const val SyncHeroResponse = "SYNC_HERO_RESPONSE"
    const val SyncHouseResponse = "SYNC_HOUSE_RESPONSE"
    const val SyncDepositResponse = "SYNC_DEPOSIT_RESPONSE"
    
}