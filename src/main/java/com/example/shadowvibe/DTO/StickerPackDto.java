package com.example.shadowvibe.DTO;

import java.util.ArrayList;
import java.util.List;

public class StickerPackDto {
    private Long id;
    private String name;
    private String authorUsername;
    private boolean system;
    private boolean mine;
    private List<StickerDto> stickers = new ArrayList<>();

    public StickerPackDto() {
    }

    public StickerPackDto(Long id, String name, String authorUsername, boolean system, boolean mine, List<StickerDto> stickers) {
        this.id = id;
        this.name = name;
        this.authorUsername = authorUsername;
        this.system = system;
        this.mine = mine;
        this.stickers = stickers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public List<StickerDto> getStickers() {
        return stickers;
    }

    public void setStickers(List<StickerDto> stickers) {
        this.stickers = stickers;
    }
}
