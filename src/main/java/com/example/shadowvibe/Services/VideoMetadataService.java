package com.example.shadowvibe.Services;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Достаёт название и заставку видео по ссылке (oEmbed/og-метаданные).
 * Используется, чтобы в ленте активных комнат показывать, что сейчас играет.
 */
@Service
public class VideoMetadataService {

    private static final Pattern OG_TITLE = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE = Pattern.compile("<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public record VideoInfo(String title, String thumb) {
    }

    /**
     * Возвращает название и заставку. thumb может быть пустым, если платформа
     * не отдаёт картинку без API-ключа (например, Vimeo по прямой ссылке).
     */
    public VideoInfo fetch(String url) {
        if (url == null || url.isBlank()) {
            return new VideoInfo(null, null);
        }
        String trimmed = url.trim();
        try {
            if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
                return fromJson(fetchText("https://www.youtube.com/oembed?format=json&url="
                        + URLEncoder.encode(trimmed, StandardCharsets.UTF_8)));
            }
            if (trimmed.contains("rutube.ru")) {
                return fromJson(fetchText("https://rutube.ru/api/oembed/?url="
                        + URLEncoder.encode(trimmed, StandardCharsets.UTF_8)));
            }
            if (trimmed.contains("vimeo.com")) {
                return fromJson(fetchText("https://vimeo.com/api/oembed.json?url="
                        + URLEncoder.encode(trimmed, StandardCharsets.UTF_8)));
            }
            if (trimmed.contains("vk.com/video") || trimmed.contains("vkvideo.ru")) {
                return fromHtml(fetchText(trimmed, "https://vk.com/"));
            }
            if (trimmed.toLowerCase().endsWith(".mp4") || trimmed.toLowerCase().endsWith(".webm")
                    || trimmed.toLowerCase().endsWith(".ogv") || trimmed.toLowerCase().endsWith(".ogg")
                    || trimmed.toLowerCase().endsWith(".mov")) {
                String name = fileName(trimmed);
                return new VideoInfo(name == null ? "Видеофайл" : name, null);
            }
        } catch (IOException e) {
            // нет метаданных — вернём null, клиент покажет заглушку
        }
        return new VideoInfo(null, null);
    }

    private VideoInfo fromJson(String json) {
        String title = extractJsonField(json, "title");
        String thumb = extractJsonField(json, "thumbnail_url");
        if (title == null && thumb == null) {
            return new VideoInfo(null, null);
        }
        return new VideoInfo(title, thumb);
    }

    private String extractJsonField(String json, String field) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!m.find()) {
            return null;
        }
        String value = m.group(1);
        value = value.replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        return decodeUnicode(value);
    }

    private String decodeUnicode(String value) {
        Matcher u = Pattern.compile("\\\\u([0-9A-Fa-f]{4})").matcher(value);
        StringBuffer sb = new StringBuffer();
        while (u.find()) {
            u.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(u.group(1), 16))));
        }
        u.appendTail(sb);
        return sb.toString();
    }

    private VideoInfo fromHtml(String html) {
        String title = null;
        String thumb = null;
        Matcher t = OG_TITLE.matcher(html);
        if (t.find()) {
            title = htmlDecode(t.group(1));
        }
        Matcher i = OG_IMAGE.matcher(html);
        if (i.find()) {
            thumb = htmlDecode(i.group(1));
        }
        if (title == null && thumb == null) {
            return new VideoInfo(null, null);
        }
        return new VideoInfo(title, thumb);
    }

    private String htmlDecode(String value) {
        return value.replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private String fileName(String url) {
        try {
            String path = new URL(url).getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            return last.isBlank() ? null : last;
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchText(String url, String... referer) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        if (referer != null && referer.length > 0 && referer[0] != null && !referer[0].isBlank()) {
            conn.setRequestProperty("Referer", referer[0]);
        }
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            conn.disconnect();
            throw new IOException("Upstream status " + code);
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
