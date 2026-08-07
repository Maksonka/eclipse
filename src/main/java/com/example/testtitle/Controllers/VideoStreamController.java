package com.example.testtitle.Controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/video")
public class VideoStreamController {

    private static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of("rutube.ru", "rtbcdn.ru", "vkuser.net", "okcdn.ru");
    private static final Pattern VIDEO_BALANCER_DEFAULT = Pattern.compile("\"video_balancer\"\\s*:\\s*\\{[^}]*\"default\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URI_ATTR = Pattern.compile("URI=\"(https?://[^\"]+)\"");
    private static final int[] VK_QUALITIES = {1080, 720, 480, 360, 240, 144};
    private static final Pattern VK_HLS = Pattern.compile("\"hls\":\"([^\"]+)\"");

    @GetMapping("/play/rutube")
    public ResponseEntity<byte[]> playRutube(@RequestParam("id") String id) {
        try {
            String encodedId = URLEncoder.encode(id, StandardCharsets.UTF_8);
            String optionsUrl = "https://rutube.ru/api/play/options/" + encodedId + "/?format=json&no_404=true";
            String referer = "https://rutube.ru/play/embed/" + encodedId + "/";
            String optionsJson = fetchText(optionsUrl, referer);
            Matcher m = VIDEO_BALANCER_DEFAULT.matcher(optionsJson);
            if (!m.find()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            String masterUrl = m.group(1).replace("\\u0026", "&");
            String master = fetchText(masterUrl, referer);
            String rewritten = rewritePlaylist(master, masterUrl);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                    .header("Cache-Control", "no-store")
                    .body(rewritten.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/play/vk")
    public ResponseEntity<String> playVk(@RequestParam("id") String id) {
        try {
            String[] parts = id.split("_", 2);
            if (parts.length != 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            String embedUrl = "https://vk.com/video_ext.php?oid=" + parts[0] + "&id=" + parts[1];
            String page = fetchText(embedUrl, "https://vk.com/");
            String kind = "mp4";
            String url = null;
            for (int q : VK_QUALITIES) {
                Matcher m = Pattern.compile("\"mp4_" + q + "\":\"([^\"]+)\"").matcher(page);
                if (m.find()) {
                    url = m.group(1).replace("\\/", "/");
                    break;
                }
            }
            if (url == null) {
                Matcher h = VK_HLS.matcher(page);
                if (h.find()) {
                    url = h.group(1).replace("\\/", "/");
                    kind = "hls";
                }
            }
            if (url == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            String proxy = "/api/video/stream?src=" + URLEncoder.encode(url, StandardCharsets.UTF_8)
                    + "&referer=" + URLEncoder.encode("https://vk.com/", StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(kind + "|" + proxy);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(@RequestParam("src") String src,
                                                        @RequestParam(value = "referer", required = false) String referer,
                                                        HttpServletRequest request) {
        try {
            URL url = new URL(src);
            if (!isAllowedHost(url.getHost())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String effReferer = (referer == null || referer.isBlank()) && isVkHost(url.getHost())
                    ? "https://vk.com/" : referer;
            HttpURLConnection conn = open(url, effReferer);
            String range = request.getHeader("Range");
            if (range != null) {
                conn.setRequestProperty("Range", range);
            }
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                conn.disconnect();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            String contentType = conn.getContentType();

            if (contentType != null && contentType.toLowerCase().contains("mpegurl")) {
                String rewritten;
                try (InputStream in = conn.getInputStream()) {
                    rewritten = rewritePlaylist(new String(readAll(in), StandardCharsets.UTF_8), src);
                }
                conn.disconnect();
                String finalBody = rewritten;
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                        .header("Cache-Control", "no-store")
                        .body(out -> out.write(finalBody.getBytes(StandardCharsets.UTF_8)));
            }

            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
            ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.valueOf(code))
                    .contentType(mediaType)
                    .header("Accept-Ranges", "bytes");
            String contentLength = conn.getHeaderField("Content-Length");
            if (contentLength != null) {
                response.header("Content-Length", contentLength);
            }
            String contentRange = conn.getHeaderField("Content-Range");
            if (contentRange != null) {
                response.header("Content-Range", contentRange);
            }
            return response.body(out -> {
                try (InputStream in = conn.getInputStream()) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
            });
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private boolean isAllowedHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String h = host.toLowerCase();
        for (String suffix : ALLOWED_HOST_SUFFIXES) {
            if (h.equals(suffix) || h.endsWith("." + suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVkHost(String host) {
        if (host == null || host.isBlank()) return false;
        String h = host.toLowerCase();
        return h.equals("vkuser.net") || h.endsWith(".vkuser.net")
                || h.equals("okcdn.ru") || h.endsWith(".okcdn.ru");
    }

    private String rewritePlaylist(String text, String baseUrl) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                sb.append(toProxy(trimmed)).append('\n');
            } else if (line.contains("URI=\"")) {
                Matcher m = URI_ATTR.matcher(line);
                StringBuffer out = new StringBuffer();
                while (m.find()) {
                    m.appendReplacement(out, "URI=\"" + Matcher.quoteReplacement(toProxy(m.group(1))) + "\"");
                }
                m.appendTail(out);
                sb.append(out.toString()).append('\n');
            } else if (!trimmed.startsWith("#") && !trimmed.isEmpty()) {
                try {
                    String absolute = new URL(new URL(baseUrl), trimmed).toExternalForm();
                    sb.append(toProxy(absolute)).append('\n');
                } catch (Exception e) {
                    sb.append(line).append('\n');
                }
            } else {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private String toProxy(String url) {
        return "/api/video/stream?src=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private HttpURLConnection open(URL url, String referer) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        if (referer != null && !referer.isBlank()) {
            conn.setRequestProperty("Referer", referer);
        }
        return conn;
    }

    private String fetchText(String url, String referer) throws IOException {
        return new String(fetchBytes(url, referer), StandardCharsets.UTF_8);
    }

    private byte[] fetchBytes(String url, String referer) throws IOException {
        HttpURLConnection conn = open(new URL(url), referer);
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            conn.disconnect();
            throw new IOException("Upstream status " + code);
        }
        try (InputStream in = conn.getInputStream()) {
            byte[] data = readAll(in);
            conn.disconnect();
            return data;
        }
    }

    private byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
