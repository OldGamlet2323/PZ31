package org.example.pz31.service;

import org.example.pz31.dto.PlaceRequest;
import org.example.pz31.exception.GeocodingUnavailableException;
import org.example.pz31.model.Place;
import org.example.pz31.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Інтеграційний тест стійкості до збоїв на РЕАЛЬНОМУ контексті з реальними транзакціями.
 * Юніт-тест із моком PlaceService цього не ловив: мок не бере участі в транзакції Spring,
 * тож пастку "rollback-only" (яка давала 500) видно лише тут. Мокаємо тільки NominatimClient.
 */
@SpringBootTest
@DisplayName("PlaceService — стійкість до збоїв API (інтеграційно, реальні транзакції)")
class PlaceServiceResilienceTest {

    @Autowired
    private PlaceService placeService;

    @Autowired
    private PlaceRepository placeRepository;

    @MockitoBean
    private NominatimClient nominatimClient;

    @Test
    @DisplayName("Nominatim недоступний → місце ЗБЕРІГАЄТЬСЯ без координат, без винятку (не 500)")
    void apiDownStillPersistsPlace() {
        when(nominatimClient.geocode(anyString()))
                .thenThrow(new GeocodingUnavailableException("down"));

        Place[] holder = new Place[1];
        assertThatCode(() ->
                holder[0] = placeService.create(
                        new PlaceRequest("Offline", "some address, Kyiv", "cafe", null, null))
        ).doesNotThrowAnyException();

        assertThat(holder[0].getId()).isNotNull();
        assertThat(holder[0].isGeocoded()).isFalse();
        // реально лежить у БД
        assertThat(placeRepository.findById(holder[0].getId()))
                .get().extracting(Place::isGeocoded).isEqualTo(false);
    }

    @Test
    @DisplayName("Адресу не знайдено → місце зберігається без координат")
    void addressNotFoundStillPersists() {
        when(nominatimClient.geocode(anyString())).thenReturn(Optional.empty());

        Place created = placeService.create(
                new PlaceRequest("Ghost", "nonexistent address 12345", null, null, null));

        assertThat(created.getId()).isNotNull();
        assertThat(created.isGeocoded()).isFalse();
        assertThat(created.getLatitude()).isNull();
    }
}
