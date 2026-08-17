package org.example.pz31.controler;

import jakarta.validation.Valid;
import org.example.pz31.dto.GeoJson;
import org.example.pz31.dto.NearbyPlaceResponse;
import org.example.pz31.dto.PagedResponse;
import org.example.pz31.dto.PlaceRequest;
import org.example.pz31.dto.PlaceResponse;
import org.example.pz31.model.Place;
import org.example.pz31.service.PlaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /** Створити місце (координати підтягнуться з Nominatim, якщо не передані вручну). */
    @PostMapping
    public ResponseEntity<PlaceResponse> create(@Valid @RequestBody PlaceRequest request) {
        Place created = placeService.create(request);
        return ResponseEntity
                .created(URI.create("/places/" + created.getId()))
                .body(PlaceResponse.from(created));
    }

    /** Список із фільтрами (category, name) та пагінацією (page, size, sort). */
    @GetMapping
    public PagedResponse<PlaceResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Place> page = placeService.list(category, name, pageable);
        return PagedResponse.from(page, PlaceResponse::from);
    }

    /** Пошук поблизу за Haversine, відсортований за відстанню. */
    @GetMapping("/nearby")
    public List<NearbyPlaceResponse> nearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5") double radiusKm,
            @RequestParam(required = false) String category) {
        return placeService.findNearby(lat, lon, radiusKm, category);
    }

    /** GeoJSON усіх місць із координатами — для відображення на карті (Leaflet). */
    @GetMapping("/geojson")
    public GeoJson.FeatureCollection geoJson() {
        return GeoJson.FeatureCollection.of(placeService.allWithCoordinates());
    }

    @GetMapping("/{id}")
    public PlaceResponse get(@PathVariable Long id) {
        return PlaceResponse.from(placeService.getEntity(id));
    }

    @PutMapping("/{id}")
    public PlaceResponse update(@PathVariable Long id, @Valid @RequestBody PlaceRequest request) {
        return PlaceResponse.from(placeService.update(id, request));
    }

    /** Догеокодувати місце, збережене раніше без координат. */
    @PostMapping("/{id}/regeocode")
    public PlaceResponse regeocode(@PathVariable Long id) {
        return PlaceResponse.from(placeService.regeocode(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        placeService.delete(id);
    }
}
