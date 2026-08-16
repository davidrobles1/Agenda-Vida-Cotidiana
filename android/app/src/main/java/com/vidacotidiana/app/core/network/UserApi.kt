package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class CurrentUser(
    val id: String,
    val email: String,
    val username: String,
    val deletionStatus: String,
)

interface UserApi {
    @GET("me")
    suspend fun getCurrentUser(): CurrentUser
}
