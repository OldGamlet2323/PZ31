package org.example.pz31.service;

import lombok.extern.slf4j.Slf4j;
import org.example.pz31.dto.Coordinates;
import org.example.pz31.dto.NominatimResult;
import org.example.pz31.exception.GeocodingUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;


@Component
@Slf4j
public class NominatimClient {

    private final RestClient restClient;
    private final RateLimiter rateLimiter;

    public NominatimClient(RestClient nominatimRestClient, RateLimiter rateLimiter) {
        this.restClient = nominatimRestClient;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Пряме геокодування: адреса → координати. Бере перший (найрелевантніший) результат.
     */
    public Optional<Coordinates> geocode(String address) {
        rateLimiter.acquire();
        try {
            NominatimResult[] results = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", address)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(NominatimResult[].class);

            if (results == null || results.length == 0) {
                log.info("Nominatim: адресу не знайдено: {}", address);
                return Optional.empty();
            }
            return Optional.of(toCoordinates(results[0]));

        } catch (RestClientException e) {
            log.warn("Nominatim недоступний при геокодуванні '{}': {}", address, e.getMessage());
            throw new GeocodingUnavailableException(
                    "Сервіс геокодування (Nominatim) тимчасово недоступний", e);
        }
    }

    /**
     * Зворотне геокодування (бонус): координати → адреса.
     */
    public Optional<String> reverse(double lat, double lon) {
        rateLimiter.acquire();
        try {
            NominatimResult result = restClient.get()
                    .uri(uri -> uri.path("/reverse")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .body(NominatimResult.class);

            if (result == null || result.displayName() == null || result.displayName().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(result.displayName());

        } catch (RestClientException e) {
            log.warn("Nominatim недоступний при reverse ({}, {}): {}", lat, lon, e.getMessage());
            throw new GeocodingUnavailableException(
                    "Сервіс геокодування (Nominatim) тимчасово недоступний", e);
        }
    }

    private Coordinates toCoordinates(NominatimResult r) {
        try {
            return new Coordinates(
                    Double.parseDouble(r.lat()),
                    Double.parseDouble(r.lon()),
                    r.displayName());
        } catch (NumberFormatException | NullPointerException e) {
            throw new GeocodingUnavailableException(
                    "Некоректна відповідь Nominatim (не вдалося розпарсити координати)", e);
        }
    }
}
