package com.example.shadowvibe.DTO;

public class ChatMessageRequest {
    private String receiverUsername;
    private String content;
    private Long replyToMessageId;
    private String stickerCode;
    private String audioUrl;
    private Long audioDurationMs;

    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getStickerCode() { return stickerCode; }
    public void setStickerCode(String stickerCode) { this.stickerCode = stickerCode; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Long getAudioDurationMs() { return audioDurationMs; }
    public void setAudioDurationMs(Long audioDurationMs) { this.audioDurationMs = audioDurationMs; }
}
