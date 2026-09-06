package com.example.shadowvibe.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    private static final String STATUS_SUCCEEDED = "succeeded";

    private final String shopId;
    private final String secretKey;
    private final String apiUrl;
    private final String returnUrl;
    private final int amountRub;
    private final int days;

    private final PremiumService premiumService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Set<String> processedPayments = ConcurrentHashMap.newKeySet();

    public PaymentService(@Value("${app.payment.shop-id:}") String shopId,
                          @Value("${app.payment.secret-key:}") String secretKey,
                          @Value("${app.payment.api-url:https://api.yookassa.ru/v3}") String apiUrl,
                          @Value("${app.payment.return-url:http://localhost:1010/premium}") String returnUrl,
                          @Value("${app.payment.amount:99}") int amountRub,
                          @Value("${app.payment.days:30}") int days,
                          PremiumService premiumService) {
        this.shopId = shopId;
        this.secretKey = secretKey;
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.returnUrl = returnUrl;
        this.amountRub = amountRub;
        this.days = days;
        this.premiumService = premiumService;
    }

    public boolean isConfigured() {
        return shopId != null && !shopId.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    public int getDays() {
        return days;
    }

    public int getAmountRub() {
        return amountRub;
    }

    public static class CreatedPayment {
        public final String id;
        public final String status;
        public final String confirmationUrl;

        public CreatedPayment(String id, String status, String confirmationUrl) {
            this.id = id;
            this.status = status;
            this.confirmationUrl = confirmationUrl;
        }
    }

    public CreatedPayment createPayment(String username) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Оплата пока не настроена: добавьте ключи ЮKassa (app.payment.shop-id / secret-key)");
        }
        ObjectNode amount = objectMapper.createObjectNode();
        amount.put("value", amountRub + ".00");
        amount.put("currency", "RUB");

        ObjectNode confirmation = objectMapper.createObjectNode();
        confirmation.put("type", "redirect");
        confirmation.put("return_url", returnUrl);

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("username", username);
        metadata.put("days", String.valueOf(days));

        ObjectNode body = objectMapper.createObjectNode();
        body.set("amount", amount);
        body.put("capture", true);
        body.set("confirmation", confirmation);
        body.put("description", "ShadowVibe Premium — " + days + " дней");
        body.set("metadata", metadata);

        HttpResponse<String> resp = send("POST", "/payments", body.toString(), UUID.randomUUID().toString());
        checkResponse(resp);

        JsonNode json = objectMapper.readTree(resp.body());
        String id = json.path("id").asText(null);
        String status = json.path("status").asText(null);
        String confirmationUrl = json.path("confirmation").path("confirmation_url").asText(null);
        return new CreatedPayment(id, status, confirmationUrl);
    }

    public String getStatus(String paymentId) throws Exception {
        if (paymentId == null || paymentId.isBlank()) return null;
        HttpResponse<String> resp = send("GET", "/payments/" + paymentId, null, null);
        checkResponse(resp);
        JsonNode json = objectMapper.readTree(resp.body());
        return json.path("status").asText(null);
    }

    public boolean activateIfPaid(String paymentId, String username) throws Exception {
        if (paymentId == null || paymentId.isBlank()) return false;
        if (!STATUS_SUCCEEDED.equals(getStatus(paymentId))) return false;
        if (!processedPayments.add(paymentId)) return false;
        premiumService.activatePaid(username, days);
        return true;
    }

    public void handleNotification(String jsonBody) {
        if (jsonBody == null || jsonBody.isBlank()) return;
        try {
            JsonNode json = objectMapper.readTree(jsonBody);
            String event = json.path("event").asText(null);
            if (!"payment.succeeded".equals(event)) return;
            JsonNode payment = json.path("object");
            String paymentId = payment.path("id").asText(null);
            if (paymentId == null || paymentId.isBlank()) return;
            String username = payment.path("metadata").path("username").asText(null);
            if (username == null || username.isBlank()) return;
            if (processedPayments.add(paymentId)) {
                premiumService.activatePaid(username, days);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean verifySignature(String jsonBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        String expected = signatureHeader.trim();
        int idx = expected.indexOf('=');
        if (idx >= 0) expected = expected.substring(idx + 1);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(jsonBody.getBytes(StandardCharsets.UTF_8));
            String actual = HexFormat.of().formatHex(bytes);
            return actual.equalsIgnoreCase(expected);
        } catch (Exception e) {
            return false;
        }
    }

    private HttpResponse<String> send(String method, String path, String body, String idempotenceKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", basicAuth())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (idempotenceKey != null) {
            builder.header("Idempotence-Key", idempotenceKey);
        }
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String basicAuth() {
        String creds = shopId + ":" + secretKey;
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    private void checkResponse(HttpResponse<String> resp) throws Exception {
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("ЮKassa вернула ошибку: HTTP " + resp.statusCode() + " " + resp.body());
        }
    }
}