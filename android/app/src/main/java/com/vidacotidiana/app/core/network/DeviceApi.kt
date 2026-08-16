package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// No default on `platform`: kotlinx.serialization's Json (encodeDefaults = false, the
// default) omits any field whose *current value* equals its declared default —
// regardless of whether the caller passed it explicitly or relied on the default —
// so a default here silently dropped the field from every request body and the
// backend's @NotBlank validation rejected it with 400 (found for real on-device).
@Serializable
data class RegisterDeviceRequest(val platform: String, val token: String)

@Serializable
data class DevicePushToken(val id: String, val platform: String, val createdAt: String, val lastSeenAt: String)

/** AND-005 contract — POST/GET /me/devices, matches Documentacion/openapi/openapi.yaml. */
interface DeviceApi {
    @GET("me/devices")
    suspend fun listDevices(): List<DevicePushToken>

    @POST("me/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): DevicePushToken
}
