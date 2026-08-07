package com.example.testtitle.DTO;

import com.example.testtitle.enums.RoomVisibility;
import com.example.testtitle.enums.WatchRoomStatus;

import java.util.List;

public class WatchRoomPreviewDto {
    private Long roomId;
    private String roomCode;
    private String name;
    private String hostUsername;
    private String videoUrl;
    private WatchRoomStatus status;
    private long positionMs;
    private int memberCount;
    private RoomVisibility visibility;
    private String videoTitle;
    private String videoThumb;
    private List<WatchRoomMemberPreviewDto> members;

    public WatchRoomPreviewDto() {
    }

    public WatchRoomPreviewDto(Long roomId, String roomCode, String name, String hostUsername,
                               String videoUrl, WatchRoomStatus status, long positionMs, int memberCount,
                               RoomVisibility visibility, String videoTitle) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.name = name;
        this.hostUsername = hostUsername;
        this.videoUrl = videoUrl;
        this.status = status;
        this.positionMs = positionMs;
        this.memberCount = memberCount;
        this.visibility = visibility;
        this.videoTitle = videoTitle;
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

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public RoomVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(RoomVisibility visibility) {
        this.visibility = visibility;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    public String getVideoThumb() {
        return videoThumb;
    }

    public void setVideoThumb(String videoThumb) {
        this.videoThumb = videoThumb;
    }

    public List<WatchRoomMemberPreviewDto> getMembers() {
        return members;
    }

    public void setMembers(List<WatchRoomMemberPreviewDto> members) {
        this.members = members;
    }
}
