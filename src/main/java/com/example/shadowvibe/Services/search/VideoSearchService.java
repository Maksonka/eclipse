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
        List<List<VideoSearchResultDto>> perProvider = new ArrayList<>();
        for (VideoSearchProvider provider : providers) {
            try {
                List<VideoSearchResultDto> found = provider.search(query, limit);
                if (found != null && !found.isEmpty()) {
                    perProvider.add(found);
                }
            } catch (Exception ignored) {
                // платформа временно недоступна — пропускаем
            }
        }
        List<VideoSearchResultDto> merged = new ArrayList<>();
        int max = 0;
        for (List<VideoSearchResultDto> list : perProvider) {
            if (list.size() > max) {
                max = list.size();
            }
        }
        for (int i = 0; i < max && merged.size() < limit; i++) {
            for (List<VideoSearchResultDto> list : perProvider) {
                if (i < list.size()) {
                    merged.add(list.get(i));
                    if (merged.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return merged;
    }
}
