package com.senspark.game.data.model.user;

import static com.senspark.game.declare.EnumConstants.*;

public class RewardDetail {
    private final BLOCK_REWARD_TYPE _type;
    private final MODE _mode;
    private DataType _dataType;
    private float _value;

    /**
     * Lưu dữ liệu bị sai lệch khi đối soát với launch pad, sẽ được cộng khi save game và không tính theo reward percent
     */
    private float _forControlValue;

    /**
     * Cần convert lại network cho phù hợp vì tuỳ loại currency mà sẽ có network khác nhau ko phải user network nào
     * là sẽ gán currency network đó
     */
    public RewardDetail(BLOCK_REWARD_TYPE type, MODE mode, DataType currentDataType, float value) {
        _type = type;
        _mode = mode;
        _dataType = type.getDataType(currentDataType);
        _value = value;
    }

    /**
     * Dùng để gán lại cho đúng network mong muốn, ko cần check và convert lại network nữa, set cái gì gán cái đó
     */
    public RewardDetail OverrideDataType(DataType overrideDataType){
        _dataType = overrideDataType;
        return this;
    }

    public BLOCK_REWARD_TYPE getBlockRewardType() {
        return _type;
    }

    public DataType getDataType() {
        return _dataType;
    }

    public MODE getMode() {
        return _mode;
    }

    public float getValue() {
        return _value;
    }

    public void addValue(float newValue) {
        _value += newValue;
    }

    public void setValue(float value) {
        _value = value;
    }

    public float getForControlValue() {
        return _forControlValue;
    }

    public void addForControlValue(float newValue) {
        _forControlValue += newValue;
    }
}
