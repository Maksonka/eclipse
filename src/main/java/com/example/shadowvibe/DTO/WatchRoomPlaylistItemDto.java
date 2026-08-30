package com.example.shadowvibe.DTO;

public class WatchRoomPlaylistItemDto {
    private Long itemId;
    private String videoUrl;
    private String title;
    private String addedBy;
    private int position;

    public WatchRoomPlaylistItemDto() {
    }

    public WatchRoomPlaylistItemDto(Long itemId, String videoUrl, String title, String addedBy, int position) {
        this.itemId = itemId;
        this.videoUrl = videoUrl;
        this.title = title;
        this.addedBy = addedBy;
        this.position = position;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
