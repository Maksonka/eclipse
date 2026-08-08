package com.example.shadowvibe.DTO;

public class ConversationPreviewDto {
    private String partnerUsername;
    private String lastMessagePreview;
    private String lastMessageTime;
    private boolean lastMessageOutgoing;
    private String partnerAvatarFilename;
    private long unreadCount;

    public ConversationPreviewDto() {
    }

    public ConversationPreviewDto(String partnerUsername, String lastMessagePreview,
                                  String lastMessageTime, boolean lastMessageOutgoing,
                                  String partnerAvatarFilename, long unreadCount) {
        this.partnerUsername = partnerUsername;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageOutgoing = lastMessageOutgoing;
        this.partnerAvatarFilename = partnerAvatarFilename;
        this.unreadCount = unreadCount;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isLastMessageOutgoing() {
        return lastMessageOutgoing;
    }

    public void setLastMessageOutgoing(boolean lastMessageOutgoing) {
        this.lastMessageOutgoing = lastMessageOutgoing;
    }

    public String getPartnerAvatarFilename() {
        return partnerAvatarFilename;
    }

    public void setPartnerAvatarFilename(String partnerAvatarFilename) {
        this.partnerAvatarFilename = partnerAvatarFilename;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
