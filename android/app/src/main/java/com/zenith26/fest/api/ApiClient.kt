package com.zenith26.fest.api

import android.content.Context
import com.google.gson.Gson
import com.zenith26.fest.models.ApiErrorResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized API client managing Retrofit singleton and network configuration.
 */
object ApiClient {

    private const val PREFS_NAME = "zenith26_prefs"
    private const val KEY_BASE_URL = "base_url"

    // Default Android Emulator loopback to host development machine (Mobile Backend)
    const val DEFAULT_EMULATOR_URL = "http://10.0.2.2:8090/"

    private var currentBaseUrl = DEFAULT_EMULATOR_URL
    private var service: ApiService? = null
    private val gson = Gson()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentBaseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_EMULATOR_URL) ?: DEFAULT_EMULATOR_URL
        rebuildService()
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun setBaseUrl(context: Context, newUrl: String) {
        var formatted = newUrl.trim()
        if (!formatted.endsWith("/")) {
            formatted += "/"
        }
        currentBaseUrl = formatted
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, currentBaseUrl)
            .apply()
        rebuildService()
    }

    private fun rebuildService() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        service = retrofit.create(ApiService::class.java)
    }

    val api: ApiService
        get() {
            if (service == null) {
                rebuildService()
            }
            return service!!
        }

    /**
     * Parses FastAPI error detail from response body.
     */
    fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val parsed = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                parsed.detail ?: "Server responded with status ${response.code()}"
            } else {
                "Error code ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "HTTP error ${response.code()}"
        }
    }
}
