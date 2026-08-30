package com.example.shadowvibe.Services;

import com.example.shadowvibe.Models.PushSubscription;
import com.example.shadowvibe.Models.User;
import com.example.shadowvibe.Repositories.PushSubscriptionRepository;
import com.example.shadowvibe.Repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PushCrypto pushCrypto;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PushService(PushSubscriptionRepository subscriptionRepository,
                       UserRepository userRepository,
                       PushCrypto pushCrypto,
                       ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.pushCrypto = pushCrypto;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getPublicKey() {
        return pushCrypto.getVapidPublicKey();
    }

    public void register(String username, String endpoint, String p256dh, String auth) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint не может быть пустым");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        PushSubscription existing = subscriptionRepository
                .findByUserUsernameAndEndpoint(username, endpoint)
                .orElse(null);
        if (existing != null) {
            existing.setP256dh(p256dh);
            existing.setAuth(auth);
            subscriptionRepository.save(existing);
            log.info("PushSubscription refreshed for user={} endpoint={}", username, endpoint);
            return;
        }

        PushSubscription subscription = new PushSubscription();
        subscription.setUser(user);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscription.setCreatedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        log.info("PushSubscription registered for user={} endpoint={} (new)", username, endpoint);
    }

    public void unregister(String username, String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        subscriptionRepository.deleteByUserUsernameAndEndpoint(username, endpoint);
    }

    public void sendPushToUser(String username, String title, String body, String url, String tag) {
        if (username == null || username.isBlank()) {
            return;
        }
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserUsername(username);
        if (subscriptions.isEmpty()) {
            log.info("Push: no subscriptions for user={}, skip", username);
            return;
        }
        byte[] payload = toPayload(title, body, url, tag);
        for (PushSubscription subscription : subscriptions) {
            sendAsync(subscription, payload);
        }
        log.info("Push: queued {} push(es) for user={} title={}", subscriptions.size(), username, title);
    }

    private byte[] toPayload(String title, String body, String url, String tag) {
        try {
            Map<String, Object> json = Map.of(
                    "title", title == null ? "ShadowVibe" : title,
                    "body", body == null ? "" : body,
                    "url", url == null ? "/chat" : url,
                    "tag", tag == null ? "default" : tag
            );
            return objectMapper.writeValueAsBytes(json);
        } catch (Exception e) {
            return ("{\"title\":\"" + safe(title) + "\",\"body\":\"" + safe(body) + "\",\"url\":\"/chat\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private void sendAsync(PushSubscription subscription, byte[] payload) {
        CompletableFuture.runAsync(() -> send(subscription, payload));
    }

    private void send(PushSubscription subscription, byte[] payload) {
        try {
            URI uri = URI.create(subscription.getEndpoint());
            byte[] body = pushCrypto.encrypt(subscription.getP256dh(), subscription.getAuth(), payload);
            String authorization = pushCrypto.buildVapidAuthorization(subscription.getEndpoint());

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", "600")
                    .header("Authorization", authorization)
                    .header("Content-Encoding", "aes128gcm")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            log.info("Push delivered: endpoint={} status={}", subscription.getEndpoint(), status);
            if (status == 404 || status == 410) {
                subscriptionRepository.deleteById(subscription.getId());
            }
        } catch (Exception e) {
            log.warn("Push send failed: endpoint={} error={}", subscription.getEndpoint(), e.toString());
        }
    }
}
