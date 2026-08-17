package org.example.pz31.service;

import org.example.pz31.dto.Coordinates;
import org.example.pz31.dto.NearbyPlaceResponse;
import org.example.pz31.dto.PlaceRequest;
import org.example.pz31.exception.GeocodingUnavailableException;
import org.example.pz31.exception.PlaceNotFoundException;
import org.example.pz31.model.Place;
import org.example.pz31.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceService — CRUD, геокодування, пошук поблизу")
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private PlaceService service;

    private PlaceRequest req(String name, String address, String category, Double lat, Double lon) {
        return new PlaceRequest(name, address, category, lat, lon);
    }

    private Place place(Long id, double lat, double lon, String category) {
        return Place.builder().id(id).name("p" + id).address("addr").category(category)
                .latitude(lat).longitude(lon).geocoded(true).build();
    }

    @Test
    @DisplayName("Створення з ручними координатами → НЕ ходить у геокодер")
    void createWithManualCoordinates() {
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.create(req("Home", "Kyiv", "home", 50.45, 30.52));

        assertThat(result.isGeocoded()).isTrue();
        assertThat(result.getLatitude()).isEqualTo(50.45);
        verify(geocodingService, never()).geocode(anyString());
    }

    @Test
    @DisplayName("Створення за адресою → підтягує координати з геокодера")
    void createGeocodesAddress() {
        when(geocodingService.geocode("Kyiv")).thenReturn(Optional.of(new Coordinates(50.45, 30.52)));
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.create(req("Cafe", "Kyiv", "cafe", null, null));

        assertThat(result.isGeocoded()).isTrue();
        assertThat(result.getLongitude()).isEqualTo(30.52);
        verify(geocodingService).geocode("Kyiv");
    }

    @Test
    @DisplayName("Адресу не знайдено → місце зберігається без координат (geocoded=false)")
    void createSavesWithoutCoordinatesWhenNotFound() {
        when(geocodingService.geocode("Nowhere")).thenReturn(Optional.empty());
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.create(req("X", "Nowhere", null, null, null));

        assertThat(result.isGeocoded()).isFalse();
        assertThat(result.getLatitude()).isNull();
    }

    @Test
    @DisplayName("Геокодер недоступний → місце все одно зберігається (без 500)")
    void createResilientToApiFailure() {
        when(geocodingService.geocode(anyString()))
                .thenThrow(new GeocodingUnavailableException("down"));
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.create(req("X", "Kyiv", null, null, null));

        assertThat(result.isGeocoded()).isFalse();
        verify(placeRepository).save(any());
    }

    @Test
    @DisplayName("getEntity знаходить існуюче місце")
    void getEntityFound() {
        Place p = place(1L, 50.45, 30.52, "cafe");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.getEntity(1L)).isSameAs(p);
    }

    @Test
    @DisplayName("getEntity кидає PlaceNotFoundException для відсутнього id")
    void getEntityNotFound() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEntity(99L))
                .isInstanceOf(PlaceNotFoundException.class);
    }

    @Test
    @DisplayName("delete відсутнього id → PlaceNotFoundException")
    void deleteMissing() {
        when(placeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(PlaceNotFoundException.class);
        verify(placeRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete існуючого id → викликає deleteById")
    void deleteExisting() {
        when(placeRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(placeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("findNearby повертає місця в радіусі, відсортовані за відстанню")
    void findNearbySortedByDistance() {
        // Центр — Київ. p1 ближче, p2 далі, p3 поза радіусом.
        Place near = place(1L, 50.4510, 30.5240, "cafe");   // ~тут же
        Place mid = place(2L, 50.5000, 30.5500, "cafe");    // кілька км
        Place far = place(3L, 51.5000, 31.5000, "cafe");    // >100 км
        when(placeRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(mid, near, far));

        List<NearbyPlaceResponse> result = service.findNearby(50.4501, 30.5234, 10.0, null);

        // far відсіклось радіусом, near перед mid
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(0).distanceKm()).isLessThanOrEqualTo(result.get(1).distanceKm());
    }

    @Test
    @DisplayName("findNearby передає фільтр категорії в репозиторій")
    void findNearbyPassesCategoryFilter() {
        when(placeRepository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq("cafe")))
                .thenReturn(List.of());

        service.findNearby(50.45, 30.52, 5.0, "cafe");

        verify(placeRepository).findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq("cafe"));
    }

    @Test
    @DisplayName("findNearby з недодатнім радіусом → IllegalArgumentException")
    void findNearbyRejectsNonPositiveRadius() {
        assertThatThrownBy(() -> service.findNearby(50.45, 30.52, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findNearby з некоректною широтою → IllegalArgumentException")
    void findNearbyRejectsBadLat() {
        assertThatThrownBy(() -> service.findNearby(120.0, 30.52, 5.0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update зі зміною адреси → перегеокодовує")
    void updateRegeocodesOnAddressChange() {
        Place existing = Place.builder().id(1L).name("old").address("Kyiv")
                .latitude(50.0).longitude(30.0).geocoded(true).build();
        when(placeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(geocodingService.geocode("Lviv")).thenReturn(Optional.of(new Coordinates(49.84, 24.03)));
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.update(1L, req("new", "Lviv", "cafe", null, null));

        assertThat(result.getLatitude()).isEqualTo(49.84);
        verify(geocodingService).geocode("Lviv");
    }

    @Test
    @DisplayName("update з ручними координатами не викликає геокодер")
    void updateWithManualCoords() {
        Place existing = Place.builder().id(1L).name("old").address("Kyiv")
                .latitude(50.0).longitude(30.0).geocoded(true).build();
        when(placeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.update(1L, req("new", "Kyiv", "cafe", 1.0, 2.0));

        assertThat(result.getLatitude()).isEqualTo(1.0);
        verify(geocodingService, never()).geocode(anyString());
    }

    @Test
    @DisplayName("regeocode догеокодовує місце без координат")
    void regeocodeFillsCoordinates() {
        Place existing = Place.builder().id(1L).name("x").address("Kyiv").geocoded(false).build();
        when(placeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(geocodingService.geocode("Kyiv")).thenReturn(Optional.of(new Coordinates(50.45, 30.52)));
        when(placeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Place result = service.regeocode(1L);

        assertThat(result.isGeocoded()).isTrue();
        assertThat(result.getLatitude()).isEqualTo(50.45);
    }

    @Test
    @DisplayName("create нормалізує порожню категорію в null")
    void createNormalizesBlankCategory() {
        when(geocodingService.geocode(anyString())).thenReturn(Optional.of(new Coordinates(1, 2)));
        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        when(placeRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        service.create(req("X", "Kyiv", "   ", null, null));

        assertThat(captor.getValue().getCategory()).isNull();
    }
}
