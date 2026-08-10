package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;

import java.util.List;

public interface VideoSearchProvider {
    List<VideoSearchResultDto> search(String query, int limit);
}
