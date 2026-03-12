package com.senspark.game.controller;

import com.senspark.game.data.model.user.BlockMap;
import com.senspark.game.declare.EnumConstants;
import com.senspark.game.declare.GameConstants;
import com.senspark.game.utils.SimpleKeyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Not used anymore
 */
public class MapDataJava {
    private List<BlockMap> blocks;
    private long createdDate;
    private int tileset;
    private EnumConstants.MODE mode;
    private final int[][] blockWall = new int[GameConstants.MAP_MAX_COL][GameConstants.MAP_MAX_ROW];

    public MapDataJava() {
        blocks = new ArrayList<>();
        createdDate = System.currentTimeMillis();
        genBlockWall();
    }

    public List<BlockMap> getBlocks() {
        return blocks;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public void addBlockMap(BlockMap bl) {
        blocks.add(bl);
    }

    public BlockMap getBlockMap(int i, int j) {
        for (BlockMap blockMap : blocks) {
            if (blockMap.getI() == i && blockMap.getJ() == j) {
                return blockMap;
            }
        }
        return null;
    }

    public boolean removeBlockMap(BlockMap blockMap) {
        if (blockMap != null && blocks.contains(blockMap)) {
            blocks.remove(blockMap);
            return true;
        }
        return false;
    }

    public String toJson() {
        throw new UnsupportedOperationException("Remove Gson");
//        Gson gson = Utils.gson;
//        return gson.toJson(getBlocks());
    }

    /**
     * @return tra về json array [i,j,type,hp,maxHp]
     */
    public String castBlocksToJsonArray() {
        throw new UnsupportedOperationException("Remove Gson");
        // cast thanh array [i,j,type,hp,maxHp]
//        return Utils.gson.toJson(blocks.stream().map(e -> new Integer[]{e.getI(), e.getJ(), e.getType(), e.getHp(), e.getMaxHp()}).collect(Collectors.toList()));
    }

    /**
     * @param blockData List of [i,j,type,hp,maxHp]
     */
    public void setBlockFromJsonArray(List<List<Integer>> blockData) {
        this.blocks = blockData.stream().map(e -> new BlockMap(
                e.get(0),
                e.get(1),
                e.get(2),
                e.get(3),
                e.get(4)
        )).collect(Collectors.toList());
    }

    private void genBlockWall() {
        // Đặt ô tường cố định trong map 0,1
        for (int i = 1; i < GameConstants.MAP_MAX_COL; i += 2) {
            for (int j = 1; j < GameConstants.MAP_MAX_ROW; j += 2) {
                blockWall[i][j] = 1;
            }
        }
    }

    public boolean isBlockWall(int i, int j) {
        return blockWall[i][j] == 1;
    }

    public boolean isContainBlock(int i, int j) {
        BlockMap bm = getBlockMap(i, j);
        return bm != null;
    }

    public boolean canSetBoom(int i, int j) {
        if (!truePosition(i, j))
            return false;

        if (isBlockWall(i, j))
            return false;

        if (isContainBlock(i, j))
            return false;

        return true;
    }

    private Boolean truePosition(int i, int j) {
        if (i < 0 || i >= GameConstants.MAP_MAX_COL)
            return false;
        if (j < 0 || j >= GameConstants.MAP_MAX_ROW)
            return false;
        return true;
    }

    public int getTileset() {
        return tileset;
    }

    public void setTileset(int tileset) {
        this.tileset = tileset;
    }

    public EnumConstants.MODE getMode() {
        return mode;
    }

    public void setMode(EnumConstants.MODE mode) {
        this.mode = mode;
    }
    
    public boolean containBlockHPLeft() {
        for (BlockMap bm : blocks) {
            if (bm.getHp() > 0)
                return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    // Che block nhiều máu nhất.
    public void reposition() {
        List<BlockMap> bms = blocks
                .stream()
                .sorted((a, b) -> -Integer.compare(a.getMaxHp(), b.getMaxHp()))
                .collect(Collectors.toList());

        BlockMap strongest = bms.get(0);

        int col = strongest.getI();
        int row = strongest.getJ();

        List<SimpleKeyValue<Integer, Integer>> positions = new ArrayList<>();
        positions.add(new SimpleKeyValue<>(col - 1, row)); // left
        positions.add(new SimpleKeyValue<>(col + 1, row)); // right
        positions.add(new SimpleKeyValue<>(col, row + 1)); // top
        positions.add(new SimpleKeyValue<>(col, row - 1)); // bottom
        List<SimpleKeyValue<Integer, Integer>> filled = positions
                .stream()
                .filter(a -> canSetBoom(a.getKey(), a.getValue()))
                .collect(Collectors.toList());

        List<BlockMap> candidateBlocks = bms.stream()
                .skip(1)
                .filter(block -> filled.stream().noneMatch(x -> block.getI() == x.getKey() && block.getJ() == x.getValue()))
                .collect(Collectors.toList());

        List<Integer> indices =
                IntStream.range(0, candidateBlocks.size() - 1)
                        .boxed()
                        .collect(Collectors.toList());

        Collections.shuffle(indices);

        int size = filled.size();
        for (int i = 0; i < size; ++i) {
            int j = indices.get(i);
            BlockMap block = candidateBlocks.get(j);
            block.setI(filled.get(i).getKey());
            block.setJ(filled.get(i).getValue());
        }
    }

    public void updateHp(float hpPercent) {
        for (BlockMap bm : blocks) {
            int type = bm.getType();
            if (type <= 1) // Block thuong hoac block tu.
                continue;

            int maxHp = bm.getMaxHp();
            int newMaxHp = Math.round(maxHp * hpPercent);
            bm.setMaxHp(newMaxHp);

            int hp = bm.getHp();
            int newHp = Math.round(hp * hpPercent);
            bm.setHp(newHp);
        }
    }
}
