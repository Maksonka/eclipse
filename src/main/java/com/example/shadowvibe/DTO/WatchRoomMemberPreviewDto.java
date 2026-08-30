package com.example.shadowvibe.DTO;

public class WatchRoomMemberPreviewDto {
    private String username;
    private String avatarFilename;

    public WatchRoomMemberPreviewDto() {
    }

    public WatchRoomMemberPreviewDto(String username, String avatarFilename) {
        this.username = username;
        this.avatarFilename = avatarFilename;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
    }
}
