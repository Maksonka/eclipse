package com.shadowvibe.app.data.api

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.shadowvibe.app.BuildConfig
import java.util.concurrent.TimeUnit

object ApiClient {

    private var baseUrl: String = "http://192.168.0.61:1010/"
    private var retrofit: Retrofit? = null
    private var _api: ShadowVibeApi? = null
    private var _httpClient: OkHttpClient? = null
    private val cookieStore = mutableMapOf<String, MutableSet<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val existing = cookieStore.getOrPut(host) { mutableSetOf() }
            existing.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            return cookieStore[host]?.toList().orEmpty()
        }
    }

    val api: ShadowVibeApi
        get() {
            if (_api == null) {
                throw IllegalStateException("ApiClient.init(context) must be called before accessing api")
            }
            return _api!!
        }

    val httpClient: OkHttpClient
        get() {
            if (_httpClient == null) {
                throw IllegalStateException("ApiClient.init(context) must be called before accessing httpClient")
            }
            return _httpClient!!
        }

    fun init(context: Context) {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (com.shadowvibe.app.BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        _httpClient = builder.build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(_httpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        _api = retrofit!!.create(ShadowVibeApi::class.java)
    }

    fun setBaseUrl(url: String) {
        val normalizedUrl = if (url.endsWith("/")) url else "$url/"
        if (normalizedUrl == baseUrl && retrofit != null) return

        baseUrl = normalizedUrl
        _api = null
        retrofit = null

        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (com.shadowvibe.app.BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        _httpClient = builder.build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(_httpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        _api = retrofit!!.create(ShadowVibeApi::class.java)
    }

    fun getCookieString(): String {
        return cookieStore.values
            .flatten()
            .joinToString("; ") { "${it.name}=${it.value}" }
    }

    fun isLoggedIn(): Boolean {
        return cookieStore.values.any { cookies ->
            cookies.any { it.name.equals("SESSION", ignoreCase = true) || it.name.equals("JSESSIONID", ignoreCase = true) }
        }
    }

    fun clearCookies() {
        cookieStore.clear()
    }
}
