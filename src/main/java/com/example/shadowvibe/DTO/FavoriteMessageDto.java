package com.example.shadowvibe.DTO;

public class FavoriteMessageDto {
    private String type;
    private Long messageId;
    private String senderUsername;
    private String preview;
    private String chatTitle;
    private String chatAvatarFilename;
    private String chatHref;
    private String favoritedAt;
    private long sortTimestamp;
    private boolean favorited;

    public FavoriteMessageDto() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }

    public String getChatAvatarFilename() {
        return chatAvatarFilename;
    }

    public void setChatAvatarFilename(String chatAvatarFilename) {
        this.chatAvatarFilename = chatAvatarFilename;
    }

    public String getChatHref() {
        return chatHref;
    }

    public void setChatHref(String chatHref) {
        this.chatHref = chatHref;
    }

    public String getFavoritedAt() {
        return favoritedAt;
    }

    public void setFavoritedAt(String favoritedAt) {
        this.favoritedAt = favoritedAt;
    }

    public long getSortTimestamp() {
        return sortTimestamp;
    }

    public void setSortTimestamp(long sortTimestamp) {
        this.sortTimestamp = sortTimestamp;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }
}
