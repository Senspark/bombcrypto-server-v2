package com.senspark.game.utils;

import com.smartfoxserver.v2.exceptions.IErrorCode;

public enum ServerError implements IErrorCode {
    SERVER_MAINTENANCE(100),
    WRONG_VERSION(101),
    USER_BANNED(102),
    INVALID_SIGNATURE(103),
    INVALID_LOGIN_DATA(104),
    USER_LOGGED(105),
    USER_REVIEW(106),
    USERNAME_PASSWORD_INVALID(107),
    UNLICENSED_USER(108),
    INVALID_ACTIVATION_CODE(109),
    LOGIN_FAILED(110),
    KICK_BY_OTHER_DEVICE(111),
    AlREADY_LOGIN(112),
    PVP_INTERNAL_ERROR(200),
    PVP_MATCH_EXPIRED(201),
    PVP_INVALID_MATCH_HASH(202),
    PVP_INVALID_MATCH_SERVER(203);
    
    private final short _id;

    ServerError(int id) {
        _id = (short) id;
    }

    @Override
    public short getId() {
        return _id;
    }
}
