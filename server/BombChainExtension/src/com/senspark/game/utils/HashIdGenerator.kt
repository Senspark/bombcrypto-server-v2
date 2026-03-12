package com.senspark.game.utils

import org.sqids.Sqids

class HashIdGenerator(
    alphabet: String,
    private val base: Long,
    private val addition: Long,
) {
    private val _sqids = Sqids(alphabet = alphabet, minLength = 10)

    fun encode(uid: Int): String {
        if (uid < 0) {
            throw IllegalArgumentException("uid must be positive")
        }
        return _sqids.encode(listOf(base, uid.toLong() + addition))
    }

    fun decode(hashId: String): Int {
        val array = _sqids.decode(hashId.lowercase())
        return (array[1] - addition).toInt()
    }

    companion object {
        fun fromEnvKey(envKey: String): HashIdGenerator {
            val parts = envKey.split(",")
            require(parts.size == 3) { "HASH_ID_KEY must have 3 comma-separated values: ALPHABET,BASE,ADDITION" }
            return HashIdGenerator(
                alphabet = parts[0],
                base = parts[1].toLong(),
                addition = parts[2].toLong()
            )
        }
    }
}
