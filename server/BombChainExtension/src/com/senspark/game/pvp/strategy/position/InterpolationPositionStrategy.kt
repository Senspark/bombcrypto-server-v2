package com.senspark.game.pvp.strategy.position

import kotlin.math.min

class InterpolationPositionStrategy(
    private val _maxInterpolationTimeframe: Int,
    private val _minInterpolationSnapshot: Int,
    private val _maxExtrapolationTimeframe: Int,
) : IPositionStrategy {
    private val _entries = mutableListOf<Pair<Int, Pair<Float, Float>>>()


    override fun getPosition(timestamp: Int): Pair<Float, Float> {
        val (canInterpolate, interpolatedPosition) = interpolatePosition(timestamp)
        if (canInterpolate) {
            return interpolatedPosition
        }
        val (canExtrapolate, extrapolatedPosition) = extrapolatePosition(timestamp)
        return extrapolatedPosition
    }

    private fun interpolatePosition(timestamp: Int): Pair<Boolean, Pair<Float, Float>> {
        require(_entries.size > 0) { "Cannot interpolate position" }
        for (i in _entries.indices) {
            val (entryTimestamp, position) = _entries[i]
            if (entryTimestamp > timestamp) {
                continue
            }
            if (i == 0) {
                return (entryTimestamp == timestamp) to position
            }
            val (prevEntryTimestamp, prevPosition) = _entries[i - 1]
            if (position == prevPosition) {
                return true to position
            }
            val t = 1f * (timestamp - entryTimestamp) / (prevEntryTimestamp - entryTimestamp)
            val interpolatedX = lerp(position.first, prevPosition.first, t)
            val interpolatedY = lerp(position.second, prevPosition.second, t)
            return true to (interpolatedX to interpolatedY)
        }
        val (_, firstPosition) = _entries[0]
        return (false to firstPosition)
    }

    private fun extrapolatePosition(timestamp: Int): Pair<Boolean, Pair<Float, Float>> {
        require(_entries.size > 0) { "Cannot extrapolate position" }
        val (firstTimestamp, firstPosition) = _entries[0];
        if (_entries.size == 1) {
            return false to firstPosition
        }
        var delta = timestamp - firstTimestamp;
        require(delta > 0) { "Cannot extrapolate when can interpolate" }
        delta = min(delta, _maxExtrapolationTimeframe);
        val (secondTimestamp, secondPosition) = _entries[1];
        val t = 1f * delta / (firstTimestamp - secondTimestamp);
        val extrapolatedX = lerp(secondPosition.first, firstPosition.first, t);
        val extrapolatedY = lerp(secondPosition.second, firstPosition.second, t);
        return true to (extrapolatedX to extrapolatedY)
    }

    override fun addPosition(timestamp: Int, x: Float, y: Float) {
        _entries.add(timestamp to (x to y))
        _entries.sortByDescending { it.first }
        while (_entries.size > _minInterpolationSnapshot &&
            _entries[0].first - _entries[_entries.size - 1].first > _maxInterpolationTimeframe) {
            _entries.removeAt(_entries.size - 1)
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
}