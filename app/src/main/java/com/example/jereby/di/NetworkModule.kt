package com.example.jereby.di

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.jereby.network.RawService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // di/NetworkModule.kt
    @RequiresApi(Build.VERSION_CODES.O)
    @Provides @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .callTimeout(java.time.Duration.ofSeconds(20))
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .readTimeout(java.time.Duration.ofSeconds(15))
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android) Jereby/1.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .build()
                chain.proceed(req)
            }
            .build()


    @Provides @Singleton
    fun retrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.uefa.com/") // placeholder base; we'll pass full URLs
            // we mostly fetch raw HTML, converter not required
            .client(client)
            .build()

    @Provides @Singleton
    fun rawService(retrofit: Retrofit): RawService = retrofit.create(RawService::class.java)
}
