package com.example.shadowvibe.Services;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простой in-memory ограничитель попыток: после N неудач подряд ключ
 * блокируется на фиксированный интервал. Успех сбрасывает счётчик.
 */
@Service
public class AttemptRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int MAX_TRACKED_KEYS = 10_000;

    public static final String KEY_LOGIN = "login:";
    public static final String KEY_PASSWORD_CHANGE = "pwchange:";

    private static class Attempt {
        volatile int failures;
        volatile LocalDateTime lockedUntil;

        boolean isLocked(LocalDateTime now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempt attempt = attempts.get(key);
        return attempt != null && attempt.isLocked(LocalDateTime.now());
    }

    /** Сколько минут осталось до снятия блокировки (минимум 1). */
    public long remainingLockMinutes(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil == null) {
            return LOCK_MINUTES;
        }
        long minutes = Duration.between(LocalDateTime.now(), attempt.lockedUntil).toMinutes();
        return Math.max(1, minutes);
    }

    public void recordFailure(String key) {
        if (attempts.size() > MAX_TRACKED_KEYS) {
            purgeStale();
        }
        LocalDateTime now = LocalDateTime.now();
        attempts.compute(key, (k, attempt) -> {
            if (attempt == null) {
                attempt = new Attempt();
            }
            if (!attempt.isLocked(now)) {
                // после истёкшей блокировки счётчик начинается заново
                if (attempt.lockedUntil != null) {
                    attempt.failures = 0;
                    attempt.lockedUntil = null;
                }
                attempt.failures++;
                if (attempt.failures >= MAX_FAILURES) {
                    attempt.lockedUntil = now.plusMinutes(LOCK_MINUTES);
                }
            }
            return attempt;
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private void purgeStale() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Attempt> entry = it.next();
            Attempt attempt = entry.getValue();
            boolean freshFailureCounting = attempt.lockedUntil == null && attempt.failures > 0;
            boolean lockExpiredLongAgo = attempt.lockedUntil != null
                    && attempt.lockedUntil.plusHours(1).isBefore(now);
            if (!freshFailureCounting && !lockExpiredLongAgo) {
                it.remove();
            } else if (lockExpiredLongAgo) {
                it.remove();
            }
        }
    }
}
