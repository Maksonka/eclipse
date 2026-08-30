package com.example.shadowvibe.DTO;

import java.util.List;

public class ChatReadReceiptDto {
    private String readerUsername;
    private String partnerUsername;
    private List<Long> messageIds;

    public ChatReadReceiptDto() {
    }

    public ChatReadReceiptDto(String readerUsername, String partnerUsername, List<Long> messageIds) {
        this.readerUsername = readerUsername;
        this.partnerUsername = partnerUsername;
        this.messageIds = messageIds;
    }

    public String getReaderUsername() {
        return readerUsername;
    }

    public void setReaderUsername(String readerUsername) {
        this.readerUsername = readerUsername;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public List<Long> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<Long> messageIds) {
        this.messageIds = messageIds;
    }
}
