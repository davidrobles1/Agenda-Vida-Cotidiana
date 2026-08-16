package com.vidacotidiana.app.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class ReminderShare(
    val id: String,
    val reminderId: String,
    val collaboratorUserId: String,
    val status: String,
    val createdAt: String,
    val revokedAt: String? = null,
)

@Serializable
data class Invitation(
    val id: String,
    val reminderId: String,
    val invitedEmail: String? = null,
    val status: String,
    val expiresAt: String,
    val createdAt: String,
)

@Serializable
data class SharesAndInvitationsResponse(
    val shares: List<ReminderShare>,
    val invitations: List<Invitation>,
)

@Serializable
data class InvitationsPage(val items: List<Invitation>)

@Serializable
data class CreateInvitationRequest(val email: String? = null, val username: String? = null)

interface SharingApi {
    @GET("reminders/{id}/shares")
    suspend fun listSharesAndInvitations(@Path("id") reminderId: String): SharesAndInvitationsResponse

    @POST("reminders/{id}/shares")
    suspend fun createInvitation(@Path("id") reminderId: String, @Body request: CreateInvitationRequest): Invitation

    @DELETE("reminders/{id}/shares/{shareId}")
    suspend fun revokeShare(@Path("id") reminderId: String, @Path("shareId") shareId: String)

    @DELETE("invitations/{invitationId}")
    suspend fun cancelInvitation(@Path("invitationId") invitationId: String)

    @GET("me/invitations")
    suspend fun listMyInvitations(): InvitationsPage

    @POST("invitations/{invitationId}/accept")
    suspend fun acceptInvitation(@Path("invitationId") invitationId: String): ReminderShare

    @POST("invitations/{invitationId}/reject")
    suspend fun rejectInvitation(@Path("invitationId") invitationId: String): Invitation
}
