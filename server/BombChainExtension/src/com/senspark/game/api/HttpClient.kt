package com.senspark.game.api

import okhttp3.OkHttpClient

class HttpClient private constructor() {

    var client: OkHttpClient = OkHttpClient()
        private set

    private object Holder {
        val INSTANCE = HttpClient()
    }

    companion object {
        @JvmStatic
        fun getInstance(): OkHttpClient {
            return Holder.INSTANCE.client
        }

        @JvmStatic
        fun initialize(logHttpRequest: Boolean) {
            if (logHttpRequest) {
                Holder.INSTANCE.client = OkHttpClient.Builder()
                    .addInterceptor(HttpRequestLoggingInterceptor())
                    .build()
            }
        }
    }
}