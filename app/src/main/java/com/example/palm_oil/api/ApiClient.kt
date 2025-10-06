package com.example.palm_oil.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Production backend URL
    private const val BASE_URL = "https://palm-oil-backend.vercel.app/"

    // Production API key
    private const val API_KEY = "jm8Yd7wX9qzF2vL6nPpR4sV3tW1yU0oH5"

    // Vercel Blob Storage token
    private const val VERCEL_BLOB_TOKEN = "vercel_blob_rw_8xqQT8h8qtF2cZr9_qiar0P6IchynkucOrhrtT3bQgc9tCK"

    // For development: Allow testing with local backend
    private const val DEBUG_MODE = true
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: PalmOilApiService = retrofit.create(PalmOilApiService::class.java)

    fun getApiKey(): String = API_KEY

    fun getVercelBlobToken(): String = VERCEL_BLOB_TOKEN
    
    /**
     * Test connection to the backend
     */
    suspend fun testConnection(): Result<String> {
        return try {
            val response = apiService.healthCheck()
            if (response.isSuccessful) {
                Result.success("Connection successful")
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }
}
