package org.example.pz31.service;

import lombok.extern.slf4j.Slf4j;
import org.example.pz31.dto.Coordinates;
import org.example.pz31.model.GeoCache;
import org.example.pz31.repository.GeoCacheRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Геокодування з кешуванням.
 * Основний кеш — у БД ({@link GeoCache}): та сама адреса не ходить у Nominatim двічі,
 * і кеш переживає перезапуск. Зберігаємо і негативний результат (found=false).
 * Reverse-геокодування кешується через Spring Cache (@Cacheable) як другий приклад механізму.
 */
@Service
@Slf4j
public class GeocodingService {

    private final NominatimClient client;
    private final GeoCacheRepository cacheRepository;

    public GeocodingService(NominatimClient client, GeoCacheRepository cacheRepository) {
        this.client = client;
        this.cacheRepository = cacheRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Coordinates> geocode(String address) {
        String key = normalize(address);

        Optional<GeoCache> cached = cacheRepository.findByAddressKey(key);
        if (cached.isPresent()) {
            GeoCache c = cached.get();
            log.debug("Кеш-хіт геокодування для '{}' (found={})", key, c.isFound());
            return c.isFound()
                    ? Optional.of(new Coordinates(c.getLatitude(), c.getLongitude()))
                    : Optional.empty();
        }

        // Кеш-міс → йдемо в зовнішній API (може кинути GeocodingUnavailableException).
        Optional<Coordinates> result = client.geocode(address);

        // Зберігаємо результат у кеш (і позитивний, і негативний).
        result.ifPresentOrElse(
                coords -> saveCache(key, coords.latitude(), coords.longitude(), true),
                () -> saveCache(key, null, null, false)
        );
        return result;
    }


    @Cacheable(value = "reverseGeocode", key = "#lat + ',' + #lon")
    public Optional<String> reverseGeocode(double lat, double lon) {
        return client.reverse(lat, lon);
    }

    private void saveCache(String key, Double lat, Double lon, boolean found) {
        cacheRepository.save(GeoCache.builder()
                .addressKey(key)
                .latitude(lat)
                .longitude(lon)
                .found(found)
                .cachedAt(Instant.now())
                .build());
    }

    private String normalize(String address) {
        return address == null ? "" : address.trim().toLowerCase();
    }
}
