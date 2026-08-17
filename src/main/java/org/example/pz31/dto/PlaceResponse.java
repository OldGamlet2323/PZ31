package org.example.pz31.dto;

import org.example.pz31.model.Place;

import java.time.Instant;

public record PlaceResponse(
        Long id,
        String name,
        String address,
        String category,
        Double latitude,
        Double longitude,
        boolean geocoded,
        Instant createdAt
) {
    public static PlaceResponse from(Place p) {
        return new PlaceResponse(
                p.getId(),
                p.getName(),
                p.getAddress(),
                p.getCategory(),
                p.getLatitude(),
                p.getLongitude(),
                p.isGeocoded(),
                p.getCreatedAt()
        );
    }
}
