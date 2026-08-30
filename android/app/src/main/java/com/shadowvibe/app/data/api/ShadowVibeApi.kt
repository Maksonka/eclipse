package com.shadowvibe.app.data.api

import com.shadowvibe.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ShadowVibeApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<UserMap>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserMap>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, Any>>

    @GET("api/me")
    suspend fun getMe(): Response<UserMap>

    @Multipart
    @POST("api/profile/update")
    suspend fun updateProfile(
        @Part("about") about: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<Map<String, Any>>

    @GET("watch/api/rooms")
    suspend fun getWatchRooms(): Response<List<WatchRoomPreview>>

    @POST("watch/api/rooms/create")
    suspend fun createWatchRoom(@Body body: Map<String, String>): Response<WatchRoomState>

    @POST("watch/api/rooms/join")
    suspend fun joinWatchRoom(@Body body: Map<String, String>): Response<WatchRoomState>

    @POST("watch/api/rooms/{roomId}/leave")
    suspend fun leaveWatchRoom(@Path("roomId") roomId: Long): Response<Map<String, Any>>

    @GET("api/conversations")
    suspend fun getConversations(): Response<List<ConversationPreview>>

    @GET("api/conversations/{username}/messages")
    suspend fun getConversationMessages(@Path("username") username: String): Response<ConversationMessages>

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): Response<List<UserMap>>

    @GET("api/users/online")
    suspend fun getOnlineUsers(): Response<List<String>>

    @GET("api/groups")
    suspend fun getGroups(): Response<List<GroupPreview>>

    @POST("api/groups/create")
    suspend fun createGroup(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("api/groups/{groupId}")
    suspend fun getGroupDetail(@Path("groupId") groupId: String): Response<GroupDetail>

    @GET("api/groups/{groupId}/messages")
    suspend fun getGroupMessages(@Path("groupId") groupId: String): Response<List<GroupMessage>>

    @POST("api/groups/{groupId}/read")
    suspend fun markGroupRead(@Path("groupId") groupId: String): Response<Map<String, Any>>

    @POST("api/groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): Response<Map<String, Any>>

    @POST("api/groups/{groupId}/members/add")
    suspend fun addGroupMember(
        @Path("groupId") groupId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/groups/{groupId}/members/{username}/remove")
    suspend fun removeGroupMember(
        @Path("groupId") groupId: String,
        @Path("username") username: String
    ): Response<Map<String, Any>>

    @POST("api/groups/{groupId}/delete")
    suspend fun deleteGroup(@Path("groupId") groupId: String): Response<Map<String, Any>>

    @GET("api/group-invites")
    suspend fun getGroupInvites(): Response<Map<String, Any>>

    @GET("api/group-invites/count")
    suspend fun getGroupInviteCount(): Response<Map<String, Any>>

    @POST("api/group-invites/{inviteId}/accept")
    suspend fun acceptGroupInvite(@Path("inviteId") inviteId: String): Response<Map<String, Any>>

    @POST("api/group-invites/{inviteId}/decline")
    suspend fun declineGroupInvite(@Path("inviteId") inviteId: String): Response<Map<String, Any>>

    @GET("api/stickers")
    suspend fun getStickers(): Response<List<StickerPack>>

    @GET("api/favorites")
    suspend fun getFavorites(): Response<FavoritesResponse>

    @GET("api/favorites/count")
    suspend fun getFavoriteCount(): Response<Map<String, Any>>

    @GET("api/notifications/muted-chats")
    suspend fun getMutedChats(): Response<MutedChatsResponse>

    @POST("api/notifications/direct")
    suspend fun setDirectNotification(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("api/notifications/group")
    suspend fun setGroupNotification(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("api/profile/{username}/common-groups")
    suspend fun getCommonGroups(@Path("username") username: String): Response<Map<String, Any>>

    @GET("api/profile/{username}/shared-media")
    suspend fun getSharedMedia(@Path("username") username: String): Response<Map<String, Any>>

    @Multipart
    @POST("chat/{username}/attachment")
    suspend fun sendDirectAttachment(
        @Path("username") username: String,
        @Part file: MultipartBody.Part,
        @Part("content") content: RequestBody,
        @Part("replyToMessageId") replyTo: RequestBody? = null
    ): Response<Map<String, Any>>

    @Multipart
    @POST("group/{groupId}/attachment")
    suspend fun sendGroupAttachment(
        @Path("groupId") groupId: String,
        @Part file: MultipartBody.Part,
        @Part("content") content: RequestBody,
        @Part("replyToMessageId") replyTo: RequestBody? = null
    ): Response<Map<String, Any>>

    @Multipart
    @POST("api/voice/upload")
    suspend fun uploadVoice(
        @Part file: MultipartBody.Part,
        @Part("durationMs") duration: RequestBody? = null
    ): Response<Map<String, Any>>
}
