package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поиск видео на YouTube по названию без API-ключа.
 * Парсит страницу результатов (ytInitialData).
 */
@Service
public class YouTubeVideoSearchProvider implements VideoSearchProvider {

    private static final String YT_INITIAL_DATA = "var ytInitialData = ";
    private static final Pattern VIDEO_ID = Pattern.compile("\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    private static final Pattern TEXT_FIELD = Pattern.compile("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override
    public List<VideoSearchResultDto> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String html = SearchHttp.get("https://www.youtube.com/results?search_query="
                    + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8),
                    Map.of("Cookie", "CONSENT=YES+1; SOCS=CAI"));
            return parse(html, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<VideoSearchResultDto> parse(String html, int limit) {
        String json = extractInitialData(html);
        if (json == null) {
            return List.of();
        }
        List<VideoSearchResultDto> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Matcher m = VIDEO_ID.matcher(json);
        while (m.find() && results.size() < limit) {
            String videoId = m.group(1);
            if (seen.contains(videoId)) {
                continue;
            }
            seen.add(videoId);
            String tail = json.substring(m.end(), Math.min(json.length(), m.end() + 80000));
            String title = extractTitle(tail);
            if (title.isEmpty()) {
                continue;
            }
            results.add(new VideoSearchResultDto(
                    "youtube",
                    videoId,
                    title,
                    "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg",
                    "https://www.youtube.com/watch?v=" + videoId));
        }
        return results;
    }

    private String extractTitle(String tail) {
        int rIdx = tail.indexOf("\"runs\":[");
        if (rIdx < 0) {
            return "";
        }
        int close = tail.indexOf("]", rIdx);
        if (close < 0) {
            close = Math.min(tail.length(), rIdx + 4000);
        }
        Matcher m = TEXT_FIELD.matcher(tail.substring(rIdx, close + 1));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append(decodeJson(m.group(1)));
        }
        return sb.toString().trim();
    }

    private String decodeJson(String value) {
        return value.replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\u0026", "&")
                .replace("\\u003c", "<")
                .replace("\\u003e", ">");
    }

    private String extractInitialData(String html) {
        int start = html.indexOf(YT_INITIAL_DATA);
        if (start < 0) {
            return null;
        }
        start += YT_INITIAL_DATA.length();
        int depth = 0;
        for (int i = start; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return html.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
