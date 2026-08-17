package org.example.pz31.service;

import org.example.pz31.dto.Coordinates;
import org.example.pz31.exception.GeocodingUnavailableException;
import org.example.pz31.model.GeoCache;
import org.example.pz31.repository.GeoCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeocodingService — кеш у БД + мок гео-клієнта")
class GeocodingServiceTest {

    @Mock
    private NominatimClient client;

    @Mock
    private GeoCacheRepository cacheRepository;

    @InjectMocks
    private GeocodingService service;

    @Test
    @DisplayName("Кеш-міс → викликає API і зберігає позитивний результат у кеш")
    void cacheMissCallsApiAndSaves() {
        when(cacheRepository.findByAddressKey(anyString())).thenReturn(Optional.empty());
        when(client.geocode("Kyiv")).thenReturn(Optional.of(new Coordinates(50.45, 30.52)));

        Optional<Coordinates> result = service.geocode("Kyiv");

        assertThat(result).isPresent();
        assertThat(result.get().latitude()).isEqualTo(50.45);

        ArgumentCaptor<GeoCache> captor = ArgumentCaptor.forClass(GeoCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().isFound()).isTrue();
        assertThat(captor.getValue().getLatitude()).isEqualTo(50.45);
    }

    @Test
    @DisplayName("Кеш-хіт (found=true) → повертає координати, НЕ ходить в API")
    void cacheHitDoesNotCallApi() {
        GeoCache cached = GeoCache.builder()
                .addressKey("kyiv").latitude(50.45).longitude(30.52).found(true).build();
        when(cacheRepository.findByAddressKey("kyiv")).thenReturn(Optional.of(cached));

        Optional<Coordinates> result = service.geocode("Kyiv");

        assertThat(result).isPresent();
        verify(client, never()).geocode(anyString());
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Кеш-хіт негативний (found=false) → empty без виклику API")
    void negativeCacheHit() {
        GeoCache cached = GeoCache.builder().addressKey("nowhere").found(false).build();
        when(cacheRepository.findByAddressKey("nowhere")).thenReturn(Optional.of(cached));

        Optional<Coordinates> result = service.geocode("Nowhere");

        assertThat(result).isEmpty();
        verify(client, never()).geocode(anyString());
    }

    @Test
    @DisplayName("Кеш-міс + API не знайшов → зберігає негативний кеш і повертає empty")
    void notFoundSavesNegativeCache() {
        when(cacheRepository.findByAddressKey(anyString())).thenReturn(Optional.empty());
        when(client.geocode("Nowhere")).thenReturn(Optional.empty());

        Optional<Coordinates> result = service.geocode("Nowhere");

        assertThat(result).isEmpty();
        ArgumentCaptor<GeoCache> captor = ArgumentCaptor.forClass(GeoCache.class);
        verify(cacheRepository).save(captor.capture());
        assertThat(captor.getValue().isFound()).isFalse();
    }

    @Test
    @DisplayName("API недоступний → пробрасує GeocodingUnavailableException")
    void apiUnavailablePropagates() {
        when(cacheRepository.findByAddressKey(anyString())).thenReturn(Optional.empty());
        when(client.geocode(anyString()))
                .thenThrow(new GeocodingUnavailableException("down"));

        assertThatThrownBy(() -> service.geocode("Kyiv"))
                .isInstanceOf(GeocodingUnavailableException.class);
    }

    @Test
    @DisplayName("Адреса нормалізується (trim + lower-case) для ключа кешу")
    void addressNormalizedForKey() {
        when(cacheRepository.findByAddressKey("kyiv, ukraine")).thenReturn(Optional.empty());
        when(client.geocode(anyString())).thenReturn(Optional.empty());

        service.geocode("  Kyiv, UKRAINE  ");

        verify(cacheRepository).findByAddressKey("kyiv, ukraine");
    }

    @Test
    @DisplayName("Reverse-геокодування делегує клієнту")
    void reverseDelegatesToClient() {
        when(client.reverse(50.45, 30.52)).thenReturn(Optional.of("Kyiv, Ukraine"));

        Optional<String> result = service.reverseGeocode(50.45, 30.52);

        assertThat(result).contains("Kyiv, Ukraine");
    }
}
