package com.example.shadowvibe.DTO;

public class StickerDto {
    private Long id;
    private String code;
    private String url;

    public StickerDto() {
    }

    public StickerDto(Long id, String code, String url) {
        this.id = id;
        this.code = code;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
