package com.example.testtitle.DTO;

public class GroupMessageRequest {
    private Long groupId;
    private String content;

    public GroupMessageRequest() {
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
}
