package org.example.pz31.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Місце користувача («моє місце»).
 * Координати необов'язкові: місце можна зберегти без них і догеокодувати пізніше,
 * якщо Nominatim був недоступний або адресу не знайдено.
 */
@Entity
@Table(name = "places", indexes = {
        @Index(name = "idx_places_category", columnList = "category"),
        @Index(name = "idx_places_lat_lon", columnList = "latitude,longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column
    private String category;

    /** Широта. null, поки місце не геокодовано. */
    @Column
    private Double latitude;

    /** Довгота. null, поки місце не геокодовано. */
    @Column
    private Double longitude;

    /** true, якщо координати успішно підтягнуто з гео-API. */
    @Column(nullable = false)
    private boolean geocoded;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
