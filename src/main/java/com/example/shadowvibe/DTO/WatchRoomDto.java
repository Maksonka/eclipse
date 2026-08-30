package com.example.shadowvibe.DTO;

import com.example.shadowvibe.enums.RoomVisibility;
import com.example.shadowvibe.enums.WatchRoomStatus;

import java.util.List;

public class WatchRoomDto {
    private Long roomId;
    private String roomCode;
    private String name;
    private String hostUsername;
    private String videoUrl;
    private WatchRoomStatus status;
    private long positionMs;
    private long updatedAtMs;
    private List<String> members;
    private boolean restart;
    private String lastControlBy;
    private RoomVisibility visibility;

    public WatchRoomDto() {
    }

    public WatchRoomDto(Long roomId, String roomCode, String name, String hostUsername,
                        String videoUrl, WatchRoomStatus status, long positionMs,
                        long updatedAtMs, List<String> members) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.name = name;
        this.hostUsername = hostUsername;
        this.videoUrl = videoUrl;
        this.status = status;
        this.positionMs = positionMs;
        this.updatedAtMs = updatedAtMs;
        this.members = members;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public boolean isRestart() {
        return restart;
    }

    public void setRestart(boolean restart) {
        this.restart = restart;
    }

    public String getLastControlBy() {
        return lastControlBy;
    }

    public void setLastControlBy(String lastControlBy) {
        this.lastControlBy = lastControlBy;
    }

    public RoomVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(RoomVisibility visibility) {
        this.visibility = visibility;
    }
}
