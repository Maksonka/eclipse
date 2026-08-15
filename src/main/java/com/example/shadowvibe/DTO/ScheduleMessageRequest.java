package com.example.shadowvibe.DTO;

public class ScheduleMessageRequest {

    private String targetType;
    private String receiverUsername;
    private Long groupId;
    private String content;
    private Long replyToMessageId;
    private Long scheduleAt;

    public ScheduleMessageRequest() {
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
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

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Long replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public Long getScheduleAt() {
        return scheduleAt;
    }

    public void setScheduleAt(Long scheduleAt) {
        this.scheduleAt = scheduleAt;
    }
}
