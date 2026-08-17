package org.example.pz31.service;

import lombok.extern.slf4j.Slf4j;
import org.example.pz31.dto.Coordinates;
import org.example.pz31.dto.NearbyPlaceResponse;
import org.example.pz31.dto.PlaceRequest;
import org.example.pz31.exception.PlaceNotFoundException;
import org.example.pz31.model.Place;
import org.example.pz31.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Бізнес-логіка місць: CRUD, геокодування при створенні, пошук поблизу, фільтри.
 */
@Service
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final GeocodingService geocodingService;

    public PlaceService(PlaceRepository placeRepository, GeocodingService geocodingService) {
        this.placeRepository = placeRepository;
        this.geocodingService = geocodingService;
    }


    @Transactional
    public Place create(PlaceRequest req) {
        Place place = Place.builder()
                .name(req.name().trim())
                .address(req.address().trim())
                .category(normalizeCategory(req.category()))
                .createdAt(Instant.now())
                .geocoded(false)
                .build();

        if (req.hasManualCoordinates()) {
            place.setLatitude(req.latitude());
            place.setLongitude(req.longitude());
            place.setGeocoded(true);
        } else {
            applyGeocoding(place, req.address());
        }

        return placeRepository.save(place);
    }


    private void applyGeocoding(Place place, String address) {
        try {
            Optional<Coordinates> coords = geocodingService.geocode(address);
            coords.ifPresent(c -> {
                place.setLatitude(c.latitude());
                place.setLongitude(c.longitude());
                place.setGeocoded(true);
            });
            if (coords.isEmpty()) {
                log.info("Адресу '{}' не знайдено — місце збережено без координат", address);
            }
        } catch (RuntimeException e) {
            log.warn("Геокодування '{}' не вдалося ({}) — місце збережено без координат",
                    address, e.getMessage());
        }
    }


    @Transactional
    public Place regeocode(Long id) {
        Place place = getEntity(id);
        applyGeocoding(place, place.getAddress());
        return placeRepository.save(place);
    }

    @Transactional(readOnly = true)
    public Place getEntity(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));
    }

    @Transactional
    public Place update(Long id, PlaceRequest req) {
        Place place = getEntity(id);
        boolean addressChanged = !place.getAddress().equalsIgnoreCase(req.address().trim());

        place.setName(req.name().trim());
        place.setAddress(req.address().trim());
        place.setCategory(normalizeCategory(req.category()));

        if (req.hasManualCoordinates()) {
            place.setLatitude(req.latitude());
            place.setLongitude(req.longitude());
            place.setGeocoded(true);
        } else if (addressChanged || !place.isGeocoded()) {
            // Адреса змінилась або координат не було — перегеокодувати.
            place.setLatitude(null);
            place.setLongitude(null);
            place.setGeocoded(false);
            applyGeocoding(place, req.address());
        }
        return placeRepository.save(place);
    }

    @Transactional
    public void delete(Long id) {
        if (!placeRepository.existsById(id)) {
            throw new PlaceNotFoundException(id);
        }
        placeRepository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public Page<Place> list(String category, String nameLike, Pageable pageable) {
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasName = StringUtils.hasText(nameLike);

        if (hasCategory && hasName) {
            return placeRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase(
                    category.trim(), nameLike.trim(), pageable);
        }
        if (hasCategory) {
            return placeRepository.findByCategoryIgnoreCase(category.trim(), pageable);
        }
        if (hasName) {
            return placeRepository.findByNameContainingIgnoreCase(nameLike.trim(), pageable);
        }
        return placeRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public List<NearbyPlaceResponse> findNearby(double lat, double lon, double radiusKm, String category) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("radiusKm має бути додатним");
        }
        validateCoordinates(lat, lon);

        double[] box = GeoDistance.boundingBox(lat, lon, radiusKm);
        String categoryFilter = StringUtils.hasText(category) ? category.trim() : null;

        List<Place> candidates = placeRepository.findWithinBoundingBox(
                box[0], box[1], box[2], box[3], categoryFilter);

        record Measured(Place place, double dist) {
        }

        return candidates.stream()
                .map(p -> new Measured(p,
                        GeoDistance.haversineKm(lat, lon, p.getLatitude(), p.getLongitude())))
                .filter(m -> m.dist() <= radiusKm) // точне коло, а не прямокутник
                .sorted(Comparator.comparingDouble(Measured::dist))
                .map(m -> NearbyPlaceResponse.from(m.place(), m.dist()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Place> allWithCoordinates() {
        return placeRepository.findAll().stream()
                .filter(Place::hasCoordinates)
                .toList();
    }

    private void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("lat має бути в діапазоні [-90, 90]");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("lon має бути в діапазоні [-180, 180]");
        }
    }

    private String normalizeCategory(String category) {
        return StringUtils.hasText(category) ? category.trim() : null;
    }
}
