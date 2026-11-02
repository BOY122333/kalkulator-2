package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Default development endpoint or custom backend server
    var baseUrl: String = "https://example.com/netvault/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var retrofit: Retrofit = buildRetrofit()

    private fun buildRetrofit(): Retrofit {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun updateBaseUrl(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            baseUrl = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            retrofit = buildRetrofit()
        }
    }

    val api: NetVaultApi
        get() = retrofit.create(NetVaultApi::class.java)
}
