package com.example.testtitle.DTO;

public class WatchRoomControlRequest {
    private Long roomId;
    private String status;
    private Long positionMs;
    private String videoUrl;
    private Boolean restart;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPositionMs() {
        return positionMs;
    }

    public void setPositionMs(Long positionMs) {
        this.positionMs = positionMs;
    }

    public Boolean getRestart() {
        return restart;
    }

    public void setRestart(Boolean restart) {
        this.restart = restart;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
