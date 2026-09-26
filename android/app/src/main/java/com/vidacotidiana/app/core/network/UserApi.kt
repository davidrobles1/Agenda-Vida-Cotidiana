package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET

@Serializable
data class CurrentUser(
    val id: String,
    val email: String,
    val username: String,
    val deletionStatus: String,
)

/** PERSONAL o LABORAL. */
@Serializable
data class EnableModeRequest(val mode: String)

interface UserApi {
    @GET("me")
    suspend fun getCurrentUser(): CurrentUser

    /**
     * Da de baja la cuenta. Siempre la propia: el endpoint no admite otra.
     *
     * El backend lo soporta desde BE-027 y ningún cliente lo ofrecía, con el
     * agravante de que Perfil decía que se pedía «desde Configuración» y ahí
     * no estaba. Responde 202: el borrado se procesa, no es instantáneo.
     */
    @DELETE("me")
    suspend fun deleteAccount()

    /**
     * Activa un modo (PERSONAL o LABORAL) EN EL SERVIDOR.
     *
     * Hasta ahora el modo vivía solo en el teléfono, así que activar Laboral
     * en uno no se notaba en el resto de dispositivos ni en la Web.
     */
    @POST("me/modes")
    suspend fun enableMode(@Body request: EnableModeRequest): CurrentUser
}
