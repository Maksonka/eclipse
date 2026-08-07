package com.example.testtitle.DTO;

import com.example.testtitle.enums.WatchRoomStatus;

public class WatchRoomSyncDto {
    private Long roomId;
    private WatchRoomStatus status;
    private long positionMs;
    private long updatedAtMs;
    private String videoUrl;
    private boolean deleted;

    public WatchRoomSyncDto() {
    }

    public WatchRoomSyncDto(Long roomId, WatchRoomStatus status, long positionMs,
                            long updatedAtMs, String videoUrl) {
        this.roomId = roomId;
        this.status = status;
        this.positionMs = positionMs;
        this.updatedAtMs = updatedAtMs;
        this.videoUrl = videoUrl;
    }

    public WatchRoomSyncDto(Long roomId, WatchRoomStatus status, long positionMs,
                            long updatedAtMs, String videoUrl, boolean deleted) {
        this(roomId, status, positionMs, updatedAtMs, videoUrl);
        this.deleted = deleted;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public WatchRoomStatus getStatus() {
        return status;
    }

    public void setStatus(WatchRoomStatus status) {
        this.status = status;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public void setPositionMs(long positionMs) {
        this.positionMs = positionMs;
    }

    public long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public void setUpdatedAtMs(long updatedAtMs) {
        this.updatedAtMs = updatedAtMs;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
