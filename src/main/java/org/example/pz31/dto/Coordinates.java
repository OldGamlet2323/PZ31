package org.example.pz31.dto;

/**
 * Результат геокодування: пара координат + (опційно) відображувана назва з Nominatim.
 */
public record Coordinates(double latitude, double longitude, String displayName) {

    public Coordinates(double latitude, double longitude) {
        this(latitude, longitude, null);
    }
}
