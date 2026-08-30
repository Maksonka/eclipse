package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VideoSearchServiceTest {

    private VideoSearchResultDto dto(String source, int n) {
        return new VideoSearchResultDto(source, "id" + n, "Title " + n, "", "https://example.com/" + n);
    }

    @Test
    void search_mergesProvidersInterleaved() {
        VideoSearchProvider p1 = mock(VideoSearchProvider.class);
        VideoSearchProvider p2 = mock(VideoSearchProvider.class);
        when(p1.search("cat", 10)).thenReturn(List.of(dto("a", 1), dto("a", 2), dto("a", 3)));
        when(p2.search("cat", 10)).thenReturn(List.of(dto("b", 1), dto("b", 2)));

        VideoSearchService service = new VideoSearchService(List.of(p1, p2));
        List<VideoSearchResultDto> result = service.search("cat", 10);

        assertEquals(List.of("a", "b", "a", "b", "a"), result.stream().map(VideoSearchResultDto::getSource).toList());
    }

    @Test
    void search_respectsLimit() {
        VideoSearchProvider p1 = mock(VideoSearchProvider.class);
        when(p1.search("cat", 2)).thenReturn(List.of(dto("a", 1), dto("a", 2), dto("a", 3)));

        VideoSearchService service = new VideoSearchService(List.of(p1));
        List<VideoSearchResultDto> result = service.search("cat", 2);

        assertEquals(2, result.size());
    }

    @Test
    void search_skipsFailingProvider() {
        VideoSearchProvider failing = mock(VideoSearchProvider.class);
        when(failing.search("cat", 10)).thenThrow(new RuntimeException("down"));
        VideoSearchProvider ok = mock(VideoSearchProvider.class);
        when(ok.search("cat", 10)).thenReturn(List.of(dto("ok", 1)));

        VideoSearchService service = new VideoSearchService(List.of(failing, ok));
        List<VideoSearchResultDto> result = service.search("cat", 10);

        assertEquals(List.of("ok"), result.stream().map(VideoSearchResultDto::getSource).toList());
    }

    @Test
    void search_handlesNullAndEmptyFromProvider() {
        VideoSearchProvider empty = mock(VideoSearchProvider.class);
        when(empty.search("cat", 10)).thenReturn(List.of());
        VideoSearchProvider nullish = mock(VideoSearchProvider.class);
        when(nullish.search("cat", 10)).thenReturn(null);

        VideoSearchService service = new VideoSearchService(List.of(empty, nullish));
        assertTrue(service.search("cat", 10).isEmpty());
    }
}
