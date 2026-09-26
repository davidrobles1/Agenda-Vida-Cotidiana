package com.vidacotidiana.app.core.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.vidacotidiana.app.BuildConfig
import com.vidacotidiana.app.feature.auth.AuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: AuthManager): Interceptor = Interceptor { chain ->
        val token = authManager.currentAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /**
     * CACHÉ DE DISCO — lo que faltaba para que el servidor y el cliente
     * estuvieran de acuerdo.
     *
     * El backend lleva desde el principio mandando `Cache-Control: private,
     * max-age=365 días` en las imágenes del Vision Board (son inmutables: una
     * subida nueva siempre estrena id, nunca pisa la anterior). OkHttp, SIN
     * UNA CACHÉ CONFIGURADA, ignora esa cabecera por completo: cada vez que
     * se abría el tablero se volvían a bajar las mismas diecinueve fotos.
     *
     * 96 MB en el directorio de caché de la aplicación, que es el que el
     * sistema puede vaciar solo si le hace falta espacio. Las respuestas con
     * `Authorization` se cachean igualmente: es una caché privada, en el
     * dispositivo del dueño de esas imágenes.
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http"), 96L * 1024 * 1024)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator,
        cache: Cache,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .cache(cache)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideReminderApi(retrofit: Retrofit): ReminderApi = retrofit.create(ReminderApi::class.java)

    @Provides
    @Singleton
    fun provideSharingApi(retrofit: Retrofit): SharingApi = retrofit.create(SharingApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideDeviceApi(retrofit: Retrofit): DeviceApi = retrofit.create(DeviceApi::class.java)

    // Los servicios de recurso: los mismos que consume la Web. Se registran
    // aquí, junto a los que ya existían, en vez de crear un módulo aparte.

    @Provides
    @Singleton
    fun provideWarrantyApi(retrofit: Retrofit): WarrantyApi = retrofit.create(WarrantyApi::class.java)

    @Provides
    @Singleton
    fun provideMaintenanceApi(retrofit: Retrofit): MaintenanceApi = retrofit.create(MaintenanceApi::class.java)

    @Provides
    @Singleton
    fun provideSubscriptionApi(retrofit: Retrofit): SubscriptionApi = retrofit.create(SubscriptionApi::class.java)

    @Provides
    @Singleton
    fun provideInventoryApi(retrofit: Retrofit): InventoryApi = retrofit.create(InventoryApi::class.java)

    @Provides
    @Singleton
    fun provideDocumentApi(retrofit: Retrofit): DocumentApi = retrofit.create(DocumentApi::class.java)

    @Provides
    @Singleton
    fun provideFamilyApi(retrofit: Retrofit): FamilyApi = retrofit.create(FamilyApi::class.java)

    @Provides
    @Singleton
    fun provideLaboralApi(retrofit: Retrofit): LaboralApi = retrofit.create(LaboralApi::class.java)

    @Provides
    @Singleton
    fun provideDayNoteApi(retrofit: Retrofit): DayNoteApi = retrofit.create(DayNoteApi::class.java)

    @Provides
    @Singleton
    fun provideVisionBoardApi(retrofit: Retrofit): VisionBoardApi = retrofit.create(VisionBoardApi::class.java)

    /* Las tres capacidades del artefacto: ánimo (V36), pasos (V34) y progreso
       de hábito (V35). Ver `WellbeingApis.kt`. */

    @Provides
    @Singleton
    fun provideMoodApi(retrofit: Retrofit): MoodApi = retrofit.create(MoodApi::class.java)

    @Provides
    @Singleton
    fun provideReminderStepApi(retrofit: Retrofit): ReminderStepApi =
        retrofit.create(ReminderStepApi::class.java)

    @Provides
    @Singleton
    fun provideRoutineProgressApi(retrofit: Retrofit): RoutineProgressApi =
        retrofit.create(RoutineProgressApi::class.java)
}
