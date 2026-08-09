package com.example.shadowvibe.DTO;

public class GroupMessageDto {
    private Long id;
    private Long groupId;
    private String content;
    private String senderUsername;
    private String timestamp;
    private String attachmentFilename;
    private String attachmentType;
    private String attachmentOriginalName;
    private Long attachmentSize;
    private Long replyToMessageId;
    private String replyToContent;
    private String replyToSenderUsername;
    private String stickerCode;
    private String stickerUrl;
    private java.util.Set<Long> deletedByUserIds = new java.util.HashSet<>();

    public GroupMessageDto() {
    }

    public GroupMessageDto(Long id, Long groupId, String content, String senderUsername, String timestamp,
                           String attachmentFilename, String attachmentType,
                           String attachmentOriginalName, Long attachmentSize,
                           Long replyToMessageId, String replyToContent, String replyToSenderUsername,
                           java.util.Set<Long> deletedByUserIds) {
        this.id = id;
        this.groupId = groupId;
        this.content = content;
        this.senderUsername = senderUsername;
        this.timestamp = timestamp;
        this.attachmentFilename = attachmentFilename;
        this.attachmentType = attachmentType;
        this.attachmentOriginalName = attachmentOriginalName;
        this.attachmentSize = attachmentSize;
        this.replyToMessageId = replyToMessageId;
        this.replyToContent = replyToContent;
        this.replyToSenderUsername = replyToSenderUsername;
        this.deletedByUserIds = deletedByUserIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAttachmentFilename() {
        return attachmentFilename;
    }

    public void setAttachmentFilename(String attachmentFilename) {
        this.attachmentFilename = attachmentFilename;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentOriginalName() {
        return attachmentOriginalName;
    }

    public void setAttachmentOriginalName(String attachmentOriginalName) {
        this.attachmentOriginalName = attachmentOriginalName;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }

    public String getReplyToSenderUsername() { return replyToSenderUsername; }
    public void setReplyToSenderUsername(String replyToSenderUsername) { this.replyToSenderUsername = replyToSenderUsername; }

    public java.util.Set<Long> getDeletedByUserIds() { return deletedByUserIds; }
    public void setDeletedByUserIds(java.util.Set<Long> deletedByUserIds) { this.deletedByUserIds = deletedByUserIds; }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getStickerUrl() { return stickerUrl; }
    public void setStickerUrl(String stickerUrl) { this.stickerUrl = stickerUrl; }
}
