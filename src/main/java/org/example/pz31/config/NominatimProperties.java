package org.example.pz31.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Налаштування інтеграції з Nominatim (OpenStreetMap).
 * base-url, обов'язковий власний User-Agent та мінімальний інтервал між запитами
 * (правило чемного використання Nominatim: не частіше 1 запиту/сек).
 */
@ConfigurationProperties(prefix = "nominatim")
public record NominatimProperties(
        String baseUrl,
        String userAgent,
        long minIntervalMs,
        int timeoutMs
) {
    public NominatimProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://nominatim.openstreetmap.org";
        }
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "PZ31-Andriy/1.0 (educational project)";
        }
        if (minIntervalMs <= 0) {
            minIntervalMs = 1000; // 1 запит/сек
        }
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
    }
}
