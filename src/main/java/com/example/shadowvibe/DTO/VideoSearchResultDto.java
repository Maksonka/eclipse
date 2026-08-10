package com.example.shadowvibe.DTO;

public class VideoSearchResultDto {
    private String source;
    private String id;
    private String title;
    private String thumb;
    private String url;

    public VideoSearchResultDto() {
    }

    public VideoSearchResultDto(String source, String id, String title, String thumb, String url) {
        this.source = source;
        this.id = id;
        this.title = title;
        this.thumb = thumb;
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
