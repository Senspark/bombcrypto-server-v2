package com.senspark.game.utils

import kotlin.random.Random

object Obfuscate {
    /**
     * @param[plainText] is utf string
     * @param[key] is utf string
     * @return utf string
     *
     * [plainText] sẽ được chèn thêm dữ liệu vào để tăng tính random cho cipherText
     * Công thức chèn: cứ mỗi 1 kí tự sẽ chèn 1 số random vào trước nó (index số lẻ là kí tự random)
     */
    fun obfuscate(plainText: String, key: String): String {
        var resultText = ""
        for (i in plainText.indices) {
            val random = Random.nextInt(0, 10) // random từ [0 -> 9]
            resultText += "$random${plainText[i]}"
        }
        return xor(resultText, key)
    }

    /**
     * @param[cipherText] is utf string
     * @param[key] is utf string
     * @return utf string
     *
     * [cipherText] sẽ được giải mã
     * Công thức giải mã: bỏ đi kí tự có index là số lẻ (index số lẻ là kí tự random)
     */
    fun deobfuscate(cipherText: String, key: String): String {
        val firstText = xor(cipherText, key)
        var resultText = ""
        for (i in firstText.indices) {
            if (i % 2 != 0) {
                resultText += firstText[i]
            }
        }
        return resultText
    }

    /**
     * @param[plainText] is Utf string
     * @param[key] is Utf string
     * @return cipherText is Utf string
     */
    fun xor(plainText: String, key: String): String {
        val keyLength = key.length
        val dataLength = plainText.length

        val result = CharArray(dataLength)
        for (i in 0 until dataLength) {
            result[i] = (plainText[i].code xor key[i % keyLength].code).toChar()
        }
        return String(result)
    }
}