package com.example.noteshare.core.di

import com.example.noteshare.BuildConfig
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.core.network.resolveApiBaseUrl
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL: String = resolveApiBaseUrl(BuildConfig.BASE_URL)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenInterceptor: TokenInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        return OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): com.example.noteshare.feature.auth.data.AuthApi {
        return retrofit.create(com.example.noteshare.feature.auth.data.AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNoteApi(retrofit: Retrofit): com.example.noteshare.feature.feed.data.NoteApi {
        return retrofit.create(com.example.noteshare.feature.feed.data.NoteApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUploadApi(retrofit: Retrofit): com.example.noteshare.core.network.UploadApi {
        return retrofit.create(com.example.noteshare.core.network.UploadApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): com.example.noteshare.feature.profile.data.UserApi {
        return retrofit.create(com.example.noteshare.feature.profile.data.UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): com.example.noteshare.feature.notification.data.NotificationApi {
        return retrofit.create(com.example.noteshare.feature.notification.data.NotificationApi::class.java)
    }
}
