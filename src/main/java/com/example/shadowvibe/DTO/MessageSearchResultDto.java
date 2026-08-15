package com.example.shadowvibe.DTO;

public class MessageSearchResultDto {
    private Long messageId;
    private String type;
    private String senderUsername;
    private String content;
    private String timestamp;
    private String date;
    private long sortTimestamp;
    private String partnerUsername;
    private String avatarFilename;
    private Long groupId;
    private String groupName;
    private String groupAvatarFilename;
    private String url;

    public MessageSearchResultDto() {
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getSortTimestamp() {
        return sortTimestamp;
    }

    public void setSortTimestamp(long sortTimestamp) {
        this.sortTimestamp = sortTimestamp;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupAvatarFilename() {
        return groupAvatarFilename;
    }

    public void setGroupAvatarFilename(String groupAvatarFilename) {
        this.groupAvatarFilename = groupAvatarFilename;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
