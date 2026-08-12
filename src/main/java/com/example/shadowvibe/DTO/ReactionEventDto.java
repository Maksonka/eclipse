package com.example.shadowvibe.DTO;

import java.util.Map;

public class ReactionEventDto {
    private Long messageId;
    private String messageType;
    private Map<String, java.util.List<String>> reactions;

    public ReactionEventDto() {
    }

    public ReactionEventDto(Long messageId, String messageType, Map<String, java.util.List<String>> reactions) {
        this.messageId = messageId;
        this.messageType = messageType;
        this.reactions = reactions;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Map<String, java.util.List<String>> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, java.util.List<String>> reactions) {
        this.reactions = reactions;
    }
}
