package com.senspark.game.api

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

class HttpRequestLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestLog = buildString {
            append("[HTTP] --> ${request.method} ${request.url}")
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                append("\n[HTTP] Body: ${buffer.readUtf8()}")
            }
        }
        println(requestLog)

        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = (System.nanoTime() - startTime) / 1_000_000

        val responseBody = response.peekBody(Long.MAX_VALUE).string()
        println("[HTTP] <-- ${response.code} ${request.url} (${duration}ms)\n[HTTP] Body: $responseBody")

        return response
    }
}
