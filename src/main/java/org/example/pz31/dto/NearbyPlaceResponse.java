package org.example.pz31.dto;

import org.example.pz31.model.Place;

/**
 * Місце + розрахована відстань (км) до точки запиту.
 */
public record NearbyPlaceResponse(
        Long id,
        String name,
        String address,
        String category,
        Double latitude,
        Double longitude,
        double distanceKm
) {
    public static NearbyPlaceResponse from(Place p, double distanceKm) {
        return new NearbyPlaceResponse(
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getCategory(),
                p.getLatitude(),
                p.getLongitude(),
                Math.round(distanceKm * 1000.0) / 1000.0 // округлення до метрів
        );
    }
}
