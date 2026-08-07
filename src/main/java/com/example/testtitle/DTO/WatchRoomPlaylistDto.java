package com.example.testtitle.DTO;

import java.util.ArrayList;
import java.util.List;

public class WatchRoomPlaylistDto {
    private Long roomId;
    private Long currentItemId;
    private List<WatchRoomPlaylistItemDto> items = new ArrayList<>();

    public WatchRoomPlaylistDto() {
    }

    public WatchRoomPlaylistDto(Long roomId, Long currentItemId, List<WatchRoomPlaylistItemDto> items) {
        this.roomId = roomId;
        this.currentItemId = currentItemId;
        this.items = items;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getCurrentItemId() {
        return currentItemId;
    }

    public void setCurrentItemId(Long currentItemId) {
        this.currentItemId = currentItemId;
    }

    public List<WatchRoomPlaylistItemDto> getItems() {
        return items;
    }

    public void setItems(List<WatchRoomPlaylistItemDto> items) {
        this.items = items;
    }
}
