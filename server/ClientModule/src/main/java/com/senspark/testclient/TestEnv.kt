package com.senspark.testclient

import io.github.cdimascio.dotenv.dotenv
import java.io.File

/**
 * Cấu hình cho test tích hợp, đọc từ `server/ClientModule/.env`.
 *
 * File đó không được commit (`.gitignore` đã chặn `.env`); mẫu nằm ở `.env.example`. Bí mật —
 * mật khẩu database, dấu phân cách RSA — chỉ sống trong file local, không nằm trong mã nguồn:
 * repo này là mã nguồn mở, một giá trị hardcode là lộ luôn quy ước đặt bí mật của cả hệ thống.
 *
 * Biến môi trường thật vẫn được ưu tiên, để chạy trên CI không cần tạo file.
 */
object TestEnv {
    private val dotenv by lazy {
        dotenv {
            directory = moduleDir().absolutePath
            ignoreIfMissing = true
        }
    }

    /** Bắt buộc phải có; thiếu thì dừng ngay kèm chỉ dẫn, thay vì hỏng ở tận đâu đó phía sau. */
    fun required(key: String): String =
        optional(key) ?: error(
            "Thiếu `$key`. Chép server/ClientModule/.env.example thành .env rồi điền giá trị " +
                "(hoặc đặt biến môi trường $key)."
        )

    fun optional(key: String, fallback: String? = null): String? =
        System.getenv(key) ?: dotenv[key]?.takeIf { it.isNotBlank() } ?: fallback

    /**
     * Gradle chạy test với thư mục làm việc là chính module, còn IDE thì hay lấy gốc dự án —
     * nên dò cả hai thay vì phụ thuộc vào nơi tiến trình được khởi động.
     */
    private fun moduleDir(): File {
        val here = File("").absoluteFile
        if (File(here, "src/test/java/autoMine").isDirectory) return here
        val nested = File(here, "server/ClientModule")
        if (nested.isDirectory) return nested
        return here
    }
}
