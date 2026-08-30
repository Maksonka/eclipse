package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Поиск видео на Rutube через открытый эндпоинт поиска (без API-ключа).
 */
@Service
public class RutubeVideoSearchProvider implements VideoSearchProvider {

    private static final String SEARCH_URL = "https://rutube.ru/api/search/combined/video_playlist";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<VideoSearchResultDto> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String json = SearchHttp.get(SEARCH_URL
                    + "?query=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                    + "&client=wdp&page=1");
            return parse(json, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<VideoSearchResultDto> parse(String json, int limit) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode arr = root.path("results");
        List<VideoSearchResultDto> results = new ArrayList<>();
        for (JsonNode item : arr) {
            if (results.size() >= limit) {
                break;
            }
            if (!"video".equals(item.path("content_type").asText())) {
                continue;
            }
            String id = item.path("id").asText("");
            String title = item.path("title").asText("");
            if (id.isEmpty() || title.isEmpty()) {
                continue;
            }
            results.add(new VideoSearchResultDto(
                    "rutube",
                    id,
                    title,
                    item.path("thumbnail_url").asText(""),
                    "https://rutube.ru/video/" + id + "/"));
        }
        return results;
    }
}
