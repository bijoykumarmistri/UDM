package com.universal.dm.core

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * High-performance shared OkHttpClient singleton.
 *
 * Sharing a single OkHttpClient instance across the entire application:
 * - Shares the connection pool (reusing TLS/TCP sockets and HTTP/2 multiplexing)
 * - Eliminates redundant thread pools and dispatcher queues
 * - Optimizes buffer throughput and lowers memory pressure
 */
object NetworkClient {

    private val dispatcher = Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 16
    }

    private val connectionPool = ConnectionPool(
        maxIdleConnections = 32,
        keepAliveDuration = 5L,
        timeUnit = TimeUnit.MINUTES
    )

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                if (original.header("User-Agent") == null) {
                    requestBuilder.header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
}
