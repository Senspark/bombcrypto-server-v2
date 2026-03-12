package com.senspark.game.declare;

import com.smartfoxserver.v2.util.IDisconnectionReason;

public enum KickReason implements IDisconnectionReason {
    IDLE(0),
    KICK(1),
    BAN(2),
    UNKNOWN(3),
    // Hack speed hoac tan cong DDos
    HACK_CHEAT(1000),
    // Login vao nhieu sv cung luc.
    CHEAT_LOGIN(1001),
    // Cheat nang luong.
    CHEAT_STAMINA(1002),
    // Cheat script, khong choi game.
    CHEAT_TOO_MANY_REQUEST(1003),
    // Sai thoi gian tren thiet bi chay game.
    WRONG_TIMESTAMP(1004),
    // Sai key
    CHEAT_INVALID_SIGNATURE(1005),
    USER_NAME_IS_EMPTY(1006),
    // User login nhieu lan vao cung server.
    CHEAT_SPAM_LOGIN(1007),
    // User logout khong thanh cong hoac Init user khong thanh cong.
    NEED_LOGIN_AGAIN(1008),
    // Login vao server khac.
    USER_LOGGED_IN(1009),
    BLINK(1010),
    USER_DELETED(1011);
    private final int value;

    KickReason(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public byte getByteValue() {
        return (byte) this.value;
    }

    public String getValueString() {
        return String.valueOf(this.value);
    }
}
