package com.senspark.testclient

import io.github.cdimascio.dotenv.dotenv
import java.io.File

/**
 * Cấu hình cho test tích hợp, đọc từ `server/ClientModule/.env` (mẫu ở `.env.example`, không
 * commit). Biến môi trường thật được ưu tiên.
 */
object TestEnv {
    private val dotenv by lazy {
        dotenv {
            directory = moduleDir().absolutePath
            ignoreIfMissing = true
        }
    }

    fun required(key: String): String =
        optional(key) ?: error(
            "Thiếu `$key`. Chép server/ClientModule/.env.example thành .env rồi điền giá trị " +
                "(hoặc đặt biến môi trường $key)."
        )

    fun optional(key: String, fallback: String? = null): String? =
        System.getenv(key) ?: dotenv[key]?.takeIf { it.isNotBlank() } ?: fallback

    /** Gradle lấy thư mục module, IDE hay lấy gốc dự án — dò cả hai. */
    private fun moduleDir(): File {
        val here = File("").absoluteFile
        if (File(here, "src/test/java/autoMine").isDirectory) return here
        val nested = File(here, "server/ClientModule")
        if (nested.isDirectory) return nested
        return here
    }
}
