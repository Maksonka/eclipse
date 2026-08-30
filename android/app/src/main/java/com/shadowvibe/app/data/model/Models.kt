package com.shadowvibe.app.data.model

data class UserMap(
    val id: Long,
    val username: String,
    val email: String? = null,
    val avatarFilename: String? = null,
    val about: String? = null,
    val online: Boolean = false
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class ConversationPreview(
    val partnerUsername: String,
    val lastMessagePreview: String,
    val lastMessageTime: String,
    val lastMessageOutgoing: Boolean,
    val partnerAvatarFilename: String? = null,
    val unreadCount: Int = 0,
    val muted: Boolean = false
)

data class GroupPreview(
    val groupId: Long,
    val groupName: String,
    val avatarFilename: String? = null,
    val lastMessagePreview: String,
    val lastMessageTime: String,
    val lastMessageSender: String? = null,
    val unreadCount: Int = 0,
    val muted: Boolean = false
)

data class ChatMessage(
    val id: Long,
    val content: String? = null,
    val senderUsername: String,
    val receiverUsername: String,
    val timestamp: String,
    val read: Boolean = false,
    val attachmentFilename: String? = null,
    val attachmentType: String? = null,
    val attachmentOriginalName: String? = null,
    val attachmentSize: Long = 0,
    val replyToMessageId: Long? = null,
    val replyToContent: String? = null,
    val replyToSenderUsername: String? = null,
    val deleted: Boolean = false,
    val stickerCode: String? = null,
    val stickerUrl: String? = null,
    val audioUrl: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val edited: Boolean = false,
    val editedAt: String? = null,
    val forwardedFrom: String? = null,
    val pinned: Boolean = false
)

data class GroupMessage(
    val id: Long,
    val groupId: Long,
    val content: String? = null,
    val senderUsername: String,
    val timestamp: String,
    val attachmentFilename: String? = null,
    val attachmentType: String? = null,
    val attachmentOriginalName: String? = null,
    val attachmentSize: Long = 0,
    val replyToMessageId: Long? = null,
    val replyToContent: String? = null,
    val replyToSenderUsername: String? = null,
    val deletedByUserIds: List<Long> = emptyList(),
    val stickerCode: String? = null,
    val stickerUrl: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(),
    val edited: Boolean = false,
    val editedAt: String? = null,
    val forwardedFrom: String? = null,
    val pinned: Boolean = false
)

data class ConversationMessages(
    val receiver: UserMap,
    val messages: List<ChatMessage>
)

data class GroupDetail(
    val id: Long,
    val name: String,
    val avatarFilename: String? = null,
    val createdBy: String,
    val isCreator: Boolean = false,
    val members: List<UserMap> = emptyList()
)

data class GroupInvite(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val invitedBy: String,
    val invitedUser: String,
    val status: String,
    val createdAt: String
)

data class Sticker(
    val id: Long,
    val code: String,
    val url: String
)

data class StickerPack(
    val id: Long,
    val name: String,
    val authorUsername: String,
    val system: Boolean = false,
    val mine: Boolean = false,
    val added: Boolean = false,
    val stickers: List<Sticker> = emptyList()
)

data class FavoriteMessage(
    val type: String,
    val messageId: Long,
    val senderUsername: String,
    val preview: String,
    val chatTitle: String,
    val chatAvatarFilename: String? = null,
    val chatHref: String,
    val favoritedAt: String,
    val favorited: Boolean = true,
    val attachmentType: String? = null,
    val attachmentFilename: String? = null
)

data class FavoritesResponse(
    val favorites: List<FavoriteMessage>,
    val count: Int
)

data class MutedChatsResponse(
    val direct: List<String>,
    val groups: List<Long>
)

data class WatchRoomState(
    val roomId: Long? = null,
    val roomCode: String = "",
    val name: String = "",
    val hostUsername: String = "",
    val videoUrl: String? = null,
    val status: String = "PAUSED",
    val positionMs: Long = 0,
    val updatedAtMs: Long = 0,
    val members: List<String> = emptyList(),
    val restart: Boolean = false
)

data class WatchRoomPreview(
    val roomId: Long,
    val roomCode: String,
    val name: String,
    val hostUsername: String,
    val videoUrl: String? = null,
    val status: String = "PAUSED",
    val memberCount: Int = 0
)

data class WatchRoomChatMessageDto(
    val id: Long,
    val senderUsername: String,
    val content: String? = null,
    val timestamp: String,
    val stickerCode: String? = null,
    val audioUrl: String? = null
)

data class WatchPlaylistItem(
    val itemId: Long,
    val title: String? = null,
    val videoUrl: String,
    val addedBy: String? = null
)

data class WatchPlaylist(
    val currentItemId: Long? = null,
    val items: List<WatchPlaylistItem> = emptyList()
)
