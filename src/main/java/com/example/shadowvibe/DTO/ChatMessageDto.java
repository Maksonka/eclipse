package com.example.shadowvibe.DTO;

public class ChatMessageDto {
    private Long id;
    private String content;
    private String senderUsername;
    private String receiverUsername;
    private String timestamp;
    private boolean read;
    private String attachmentFilename;
    private String attachmentType;
    private String attachmentOriginalName;
    private Long attachmentSize;
    private Long replyToMessageId;
    private String replyToContent;
    private String replyToSenderUsername;
    private boolean deleted;
    private String stickerCode;
    private String stickerUrl;

    public ChatMessageDto() {
    }

    public ChatMessageDto(Long id, String content, String senderUsername, String receiverUsername,
                          String timestamp, boolean read, String attachmentFilename, String attachmentType,
                          String attachmentOriginalName, Long attachmentSize,
                          Long replyToMessageId, String replyToContent, String replyToSenderUsername, boolean deleted) {
        this.id = id;
        this.content = content;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.timestamp = timestamp;
        this.read = read;
        this.attachmentFilename = attachmentFilename;
        this.attachmentType = attachmentType;
        this.attachmentOriginalName = attachmentOriginalName;
        this.attachmentSize = attachmentSize;
        this.replyToMessageId = replyToMessageId;
        this.replyToContent = replyToContent;
        this.replyToSenderUsername = replyToSenderUsername;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
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

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getStickerUrl() { return stickerUrl; }
    public void setStickerUrl(String stickerUrl) { this.stickerUrl = stickerUrl; }
}
