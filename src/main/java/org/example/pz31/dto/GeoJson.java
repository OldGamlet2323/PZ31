package org.example.pz31.dto;

import org.example.pz31.model.Place;

import java.util.List;
import java.util.Map;

/**
 * GeoJSON (FeatureCollection) для відображення точок на карті (напр. Leaflet).
 * Формат за специфікацією RFC 7946: coordinates = [longitude, latitude].
 */
public final class GeoJson {

    private GeoJson() {
    }

    public record FeatureCollection(String type, List<Feature> features) {
        public static FeatureCollection of(List<Place> places) {
            List<Feature> features = places.stream()
                    .filter(Place::hasCoordinates)
                    .map(Feature::of)
                    .toList();
            return new FeatureCollection("FeatureCollection", features);
        }
    }

    public record Feature(String type, Geometry geometry, Map<String, Object> properties) {
        public static Feature of(Place p) {
            return new Feature(
                    "Feature",
                    new Geometry("Point", List.of(p.getLongitude(), p.getLatitude())),
                    Map.of(
                            "id", p.getId(),
                            "name", p.getName(),
                            "address", p.getAddress(),
                            "category", p.getCategory() == null ? "" : p.getCategory()
                    )
            );
        }
    }

    public record Geometry(String type, List<Double> coordinates) {
    }
}
