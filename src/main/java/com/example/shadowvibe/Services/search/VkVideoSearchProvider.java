package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Поиск видео ВКонтакте через официальный API video.search.
 * Требует токен доступа в свойстве vk.access-token (или env VK_ACCESS_TOKEN).
 * Без токена поиск по VK просто не даёт результатов.
 */
@Service
public class VkVideoSearchProvider implements VideoSearchProvider {

    private static final String SEARCH_URL = "https://api.vk.com/method/video.search";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String accessToken;

    public VkVideoSearchProvider(@Value("${vk.access-token:}") String accessToken) {
        this.accessToken = accessToken == null ? "" : accessToken.trim();
    }

    @Override
    public List<VideoSearchResultDto> search(String query, int limit) {
        if (query == null || query.isBlank() || accessToken.isEmpty()) {
            return List.of();
        }
        try {
            String url = SEARCH_URL
                    + "?q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8)
                    + "&count=" + Math.min(limit, 20)
                    + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                    + "&v=5.199";
            String json = SearchHttp.get(url);
            return parse(json, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<VideoSearchResultDto> parse(String json, int limit) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (root.has("error")) {
            return List.of();
        }
        JsonNode items = root.path("response").path("items");
        List<VideoSearchResultDto> results = new ArrayList<>();
        for (JsonNode item : items) {
            if (results.size() >= limit) {
                break;
            }
            long ownerId = item.path("owner_id").asLong();
            long videoId = item.path("id").asLong();
            String title = item.path("title").asText("");
            if (ownerId == 0 || videoId == 0 || title.isEmpty()) {
                continue;
            }
            results.add(new VideoSearchResultDto(
                    "vk",
                    ownerId + "_" + videoId,
                    title,
                    item.path("photo_320").asText(""),
                    "https://vk.com/video" + ownerId + "_" + videoId));
        }
        return results;
    }
}
