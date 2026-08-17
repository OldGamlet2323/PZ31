package org.example.pz31.controler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeoController#distance — Haversine між двома точками")
class GeoControllerTest {

    // distance() не залежить від GeocodingService, тож передаємо null.
    private final GeoController controller = new GeoController(null);

    @Test
    @DisplayName("Київ → Львів ≈ 468 км")
    void kyivToLviv() {
        Map<String, Object> res = controller.distance(50.4501, 30.5234, 49.8397, 24.0297);

        assertThat((double) res.get("distanceKm")).isCloseTo(468.0, org.assertj.core.data.Offset.offset(10.0));
        assertThat(res).containsKeys("from", "to", "distanceKm", "distanceMeters");
    }

    @Test
    @DisplayName("Однакові точки → 0 км")
    void samePointIsZero() {
        Map<String, Object> res = controller.distance(50.45, 30.52, 50.45, 30.52);
        assertThat((double) res.get("distanceKm")).isZero();
        assertThat((long) res.get("distanceMeters")).isZero();
    }

    @Test
    @DisplayName("Невалідна широта → IllegalArgumentException (→ 400)")
    void invalidLatitudeRejected() {
        assertThatThrownBy(() -> controller.distance(91.0, 30.52, 50.45, 30.52))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    @DisplayName("Невалідна довгота другої точки → IllegalArgumentException (→ 400)")
    void invalidLongitudeRejected() {
        assertThatThrownBy(() -> controller.distance(50.45, 30.52, 50.45, 200.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitude");
    }
}
