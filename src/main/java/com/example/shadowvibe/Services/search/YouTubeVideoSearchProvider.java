package com.example.shadowvibe.Services.search;

import com.example.shadowvibe.DTO.VideoSearchResultDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поиск видео на YouTube по названию без API-ключа.
 * Парсит страницу результатов (ytInitialData).
 */
@Service
public class YouTubeVideoSearchProvider implements VideoSearchProvider {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final String YT_INITIAL_DATA = "var ytInitialData = ";
    private static final Pattern VIDEO_ID = Pattern.compile("\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    private static final Pattern TEXT_FIELD = Pattern.compile("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override
    public List<VideoSearchResultDto> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String html = fetchText("https://www.youtube.com/results?search_query="
                    + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
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

    private String fetchText(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
        conn.setRequestProperty("Cookie", "CONSENT=YES+1; SOCS=CAI");
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new Exception("YouTube status " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            conn.disconnect();
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
