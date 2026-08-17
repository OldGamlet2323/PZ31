package org.example.pz31.controler;

import org.example.pz31.exception.AddressNotFoundException;
import org.example.pz31.service.GeoDistance;
import org.example.pz31.service.GeocodingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Гео-ендпоінти, не прив'язані до конкретного місця.
 */
@RestController
@RequestMapping("/geo")
public class GeoController {

    private final GeocodingService geocodingService;

    public GeoController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /** Пряме геокодування адреси (перевірка/попередній перегляд координат). */
    @GetMapping("/geocode")
    public Map<String, Object> geocode(@RequestParam String address) {
        return geocodingService.geocode(address)
                .map(c -> Map.<String, Object>of(
                        "address", address,
                        "latitude", c.latitude(),
                        "longitude", c.longitude()))
                .orElseThrow(() -> new AddressNotFoundException(address));
    }

    /** Зворотне геокодування: координати → адреса (бонус). */
    @GetMapping("/reverse")
    public Map<String, Object> reverse(@RequestParam double lat, @RequestParam double lon) {
        return geocodingService.reverseGeocode(lat, lon)
                .map(name -> Map.<String, Object>of(
                        "latitude", lat,
                        "longitude", lon,
                        "address", name))
                .orElseThrow(() -> new AddressNotFoundException(lat + "," + lon));
    }

    /** Відстань між двома довільними точками за формулою Haversine (км + метри). */
    @GetMapping("/distance")
    public Map<String, Object> distance(
            @RequestParam double lat1, @RequestParam double lon1,
            @RequestParam double lat2, @RequestParam double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        double km = GeoDistance.haversineKm(lat1, lon1, lat2, lon2);
        return Map.of(
                "from", Map.of("latitude", lat1, "longitude", lon1),
                "to", Map.of("latitude", lat2, "longitude", lon2),
                "distanceKm", Math.round(km * 1000.0) / 1000.0,
                "distanceMeters", Math.round(km * 1000.0));
    }

    private void validateCoordinates(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("latitude має бути в діапазоні [-90, 90]");
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("longitude має бути в діапазоні [-180, 180]");
        }
    }
}
