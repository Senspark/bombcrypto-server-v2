package com.senspark.game.controller

import com.senspark.game.data.model.user.BlockMap
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.GameConstants
import com.senspark.game.utils.JsonExtensionBuilder
import com.senspark.game.utils.SimpleKeyValue
import kotlinx.serialization.encodeToString
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream

class MapData {
    var blocks: MutableList<BlockMap> = ArrayList()
    var createdDate: Long = System.currentTimeMillis()
    var tileset: Int = 0
    var mode: EnumConstants.MODE = EnumConstants.MODE.PVE_V2
    private val _blockWall = Array(GameConstants.MAP_MAX_COL) { IntArray(GameConstants.MAP_MAX_ROW) }

    init {
        genBlockWall()
    }

    fun addBlockMap(bl: BlockMap) {
        blocks.add(bl)
    }

    fun addBlocks(blocks: List<BlockMap>) {
        this.blocks.addAll(blocks)
    }

    fun getBlockMap(i: Int, j: Int): BlockMap? {
        val result = blocks.firstOrNull { it.i == i && it.j == j }
        return result
    }

    fun removeBlockMap(blockMap: BlockMap?): Boolean {
        if (blockMap != null) {
            val result = blocks.remove(blockMap)
            return result
        }
        return false
    }

    fun toJson(): String {
        val result = JsonExtensionBuilder.json.encodeToString(blocks)
        return result
    }

    /**
     * @return tra về json array [i,j,type,hp,maxHp]
     */
    fun castBlocksToJsonArray(): String {
        val result =
            JsonExtensionBuilder.json.encodeToString(blocks.map { listOf(it.i, it.j, it.type, it.hp, it.maxHp) })
        return result
    }

    /**
     * @param blockData List of [i,j,type,hp,maxHp]
     */
    fun setBlockFromJsonArray(blockData: List<List<Int>>) {
        blocks = blockData.map { BlockMap(it[0], it[1], it[2], it[3], it[4]) }.toMutableList()
    }

    private fun genBlockWall() {
        // Đặt ô tường cố định trong map 0,1
        for (i in 1 until GameConstants.MAP_MAX_COL step 2) {
            for (j in 1 until GameConstants.MAP_MAX_ROW step 2) {
                _blockWall[i][j] = 1
            }
        }
    }

    fun canSetBoom(i: Int, j: Int): Boolean {
        if (!truePosition(i, j)) return false
        if (isBlockWall(i, j)) return false
        if (isContainBlock(i, j)) return false
        return true
    }

    private fun truePosition(i: Int, j: Int): Boolean {
        if (i < 0 || i >= GameConstants.MAP_MAX_COL) return false
        if (j < 0 || j >= GameConstants.MAP_MAX_ROW) return false
        return true
    }

    private fun isBlockWall(i: Int, j: Int): Boolean {
        return _blockWall[i][j] == 1
    }

    private fun isContainBlock(i: Int, j: Int): Boolean {
        return getBlockMap(i, j) != null
    }

    fun containBlockHPLeft(): Boolean {
        val result = blocks.any { it.hp > 0 }
        return result
    }

    fun isEmpty(): Boolean {
        val empty = blocks.isEmpty()
        return empty
    }

    // Che block nhiều máu nhất.
    fun reposition() {
        val bms = blocks.sortedByDescending { it.maxHp }
        val strongest = bms[0]
        val col = strongest.i
        val row = strongest.j

        val positions = listOf(
            SimpleKeyValue(col - 1, row), // left
            SimpleKeyValue(col + 1, row), // right
            SimpleKeyValue(col, row + 1), // top
            SimpleKeyValue(col, row - 1)  // bottom
        )

        val filled = positions.filter { canSetBoom(it.key, it.value) }
        val candidateBlocks = bms.drop(1).filter { block ->
            filled.none { it.key == block.i && it.value == block.j }
        }

        val indices = IntStream.range(0, candidateBlocks.size).boxed().collect(Collectors.toList())
        indices.shuffle()

        val size = filled.size
        for (i in 0 until size) {
            val j = indices[i]
            val block = candidateBlocks[j]
            block.i = filled[i].key
            block.j = filled[i].value
        }
    }
}