package org.example.pz31.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Обґрунтування вибору HTTP-клієнта:
 * використовуємо RestClient (Spring 6.1+) — сучасний синхронний fluent-клієнт.
 * Для одиничних викликів Nominatim нам не потрібна реактивність WebClient
 * (тягне зайвий webflux), а RestTemplate — legacy у режимі підтримки.
 * RestClient дає лаконічний API, вбудований парсинг JSON і зручну обробку статусів.
 *
 * Тут же централізовано задаємо обов'язковий User-Agent і таймаути.
 */
@Configuration
@EnableConfigurationProperties(NominatimProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient nominatimRestClient(NominatimProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.timeoutMs());
        factory.setReadTimeout(props.timeoutMs());

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                // Обов'язковий власний User-Agent — вимога правил Nominatim (інакше −3 бали / бан).
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }
}
