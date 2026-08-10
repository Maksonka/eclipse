package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Агрегирует результаты поиска видео по платформам.
 * Провайдеры, которые временно недоступны, просто не дают результатов.
 */
@Service
public class VideoSearchService {

    private final List<VideoSearchProvider> providers;

    public VideoSearchService(List<VideoSearchProvider> providers) {
        this.providers = providers;
    }

    public List<VideoSearchResultDto> search(String query, int limit) {
        List<VideoSearchResultDto> results = new ArrayList<>();
        for (VideoSearchProvider provider : providers) {
            try {
                List<VideoSearchResultDto> found = provider.search(query, limit);
                if (found != null) {
                    results.addAll(found);
                }
            } catch (Exception ignored) {
                // платформа временно недоступна — пропускаем
            }
            if (results.size() >= limit) {
                break;
            }
        }
        return results.size() > limit ? results.subList(0, limit) : results;
    }
}
