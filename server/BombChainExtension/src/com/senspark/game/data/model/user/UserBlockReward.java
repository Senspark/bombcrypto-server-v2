//package com.senspark.game.data.model.user;
//
//import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE;
//import com.senspark.game.declare.ErrorCode;
//import com.senspark.game.exception.CustomException;
//import com.senspark.lib.data.manager.GameConfigManager;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.stream.Collectors;
//
//import static com.senspark.game.declare.EnumConstants.DataType;
//
//public class UserBlockReward {
//    private BLOCK_REWARD_TYPE rewardType;
//    private float values;
//    private double totalValues;
//    private long lastTimeClaimSuccess;
//    private double claimPending;
//    private DataType dataType;
//
//    public UserBlockReward() {
//
//    }
//
//    public UserBlockReward(BLOCK_REWARD_TYPE rewardType, DataType dataType) {
//        this.rewardType = rewardType;
//        this.values = 0;
//        this.totalValues = 0;
//        this.lastTimeClaimSuccess = Instant.now().getEpochSecond();
//        this.claimPending = 0;
//        this.dataType = dataType;
//    }
//
//    public BLOCK_REWARD_TYPE getRewardType() {
//        return rewardType;
//    }
//
//    public void setRewardType(BLOCK_REWARD_TYPE rewardType) {
//        this.rewardType = rewardType;
//    }
//
//    public synchronized float getValues() {
//        return values;
//    }
//
//    public synchronized void setValues(float values) {
//        this.values = values;
//    }
//
//    public double getClaimPending() {
//        return claimPending;
//    }
//
//    public void setClaimPending(double claimPending) {
//        this.claimPending = claimPending;
//    }
//
//    public synchronized void addValues(float values) {
//        this.values += values;
//        this.totalValues += values;
//    }
//
//    public synchronized void subValues(float values) {
//        this.values -= values;
//    }
//
//    public long getLastTimeClaimSuccess() {
//        return lastTimeClaimSuccess;
//    }
//
//    public void setLastTimeClaimSuccess(long lastTimeClaimSuccess) {
//        this.lastTimeClaimSuccess = lastTimeClaimSuccess;
//    }
//
//    public double getTotalValues() {
//        return totalValues;
//    }
//
//    public void setTotalValues(double totalValues) {
//        this.totalValues = totalValues;
//    }
//
//    public DataType getDataType() {
//        return dataType;
//    }
//
//    public void setDataType(DataType dataType) {
//        this.dataType = dataType;
//    }
//
//    public int getRemainTimeCanClaim() {
//        long currTime = System.currentTimeMillis();
//        long nextTimeCanClaim = getLastTimeClaimSuccess() + (long) GameConfigManager.instance.nextTimeCanClaimReward * 60 * 1000; //m * 60s * 1000ms
//
//        long remainTime = nextTimeCanClaim - currTime;
//        if (remainTime <= 0) {
//            return 0;
//        } else {
//            return (int) (remainTime / 1000); //return second
//        }
//    }
//
//    public boolean canClaim() throws CustomException {
//        if (claimPending > 0) {
//            return true;
//        }
//        if (rewardType == BLOCK_REWARD_TYPE.SENSPARK) {
//            return values >= 40;
//        } else if (rewardType == BLOCK_REWARD_TYPE.BCOIN) {
//            List<List<Integer>> claimBcoinLimit = GameConfigManager.instance.claimBcoinLimit;
//            boolean result = values > 0 && claimBcoinLimit.stream().anyMatch(e -> {
//                float minvalue = e.get(0);
//                float maxValue = e.get(1);
//                return ((minvalue >= 0 && values >= minvalue) || minvalue < 0)
//                        && ((maxValue >= 0 && values <= maxValue) || maxValue < 0);
//            });
//            if (result)
//                return true;
//            else {
//                String message = claimBcoinLimit.stream().map(e -> {
//                    float minvalue = e.get(0);
//                    float maxValue = e.get(1);
//                    if (minvalue >= 0 && maxValue >= 0) {
//                        return String.format("%s <= claim value <= %s", minvalue, maxValue);
//                    } else if (minvalue > 0) {
//                        return String.format("claim value >= %s", minvalue);
//                    } else {
//                        return String.format("claim value <= %s", maxValue);
//                    }
//                }).collect(Collectors.joining(" or "));
//                throw new CustomException("Claim condition: " + message, ErrorCode.NOT_ENOUGH_REWARD);
//            }
//        } else if (rewardType == BLOCK_REWARD_TYPE.BCOIN_DEPOSITED) {
//            return values > 0;
//        } else if (rewardType == BLOCK_REWARD_TYPE.BOMBERMAN) {
//            return values > 0;
//        } else {
//            return values > 1;
//        }
//    }
//}
