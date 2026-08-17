package org.example.pz31.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoDistance — формула Haversine та bounding box")
class GeoDistanceTest {

    @Test
    @DisplayName("Відстань між однаковими точками = 0")
    void samePointIsZero() {
        double d = GeoDistance.haversineKm(50.45, 30.52, 50.45, 30.52);
        assertThat(d).isZero();
    }

    @Test
    @DisplayName("1° широти на екваторі ≈ 111.19 км")
    void oneDegreeLatitude() {
        double d = GeoDistance.haversineKm(0.0, 0.0, 1.0, 0.0);
        assertThat(d).isCloseTo(111.19, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    @DisplayName("1° довготи на екваторі ≈ 111.19 км")
    void oneDegreeLongitudeAtEquator() {
        double d = GeoDistance.haversineKm(0.0, 0.0, 0.0, 1.0);
        assertThat(d).isCloseTo(111.19, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    @DisplayName("Київ → Львів ≈ 468 км")
    void kyivToLviv() {
        double d = GeoDistance.haversineKm(50.4501, 30.5234, 49.8397, 24.0297);
        assertThat(d).isCloseTo(468.0, org.assertj.core.data.Offset.offset(10.0));
    }

    @Test
    @DisplayName("Симетричність: d(A,B) == d(B,A)")
    void isSymmetric() {
        double ab = GeoDistance.haversineKm(50.45, 30.52, 49.84, 24.03);
        double ba = GeoDistance.haversineKm(49.84, 24.03, 50.45, 30.52);
        assertThat(ab).isEqualTo(ba);
    }

    @Test
    @DisplayName("Bounding box охоплює точку і має коректний порядок меж")
    void boundingBoxContainsCenter() {
        double lat = 50.45, lon = 30.52, radius = 5.0;
        double[] box = GeoDistance.boundingBox(lat, lon, radius);

        double minLat = box[0], maxLat = box[1], minLon = box[2], maxLon = box[3];
        assertThat(minLat).isLessThan(lat);
        assertThat(maxLat).isGreaterThan(lat);
        assertThat(minLon).isLessThan(lon);
        assertThat(maxLon).isGreaterThan(lon);
    }

    @Test
    @DisplayName("Більший радіус → ширший bounding box")
    void largerRadiusWiderBox() {
        double[] small = GeoDistance.boundingBox(50.45, 30.52, 1.0);
        double[] large = GeoDistance.boundingBox(50.45, 30.52, 10.0);
        assertThat(large[1] - large[0]).isGreaterThan(small[1] - small[0]);
    }

    @Test
    @DisplayName("Bounding box не виходить за межі широти [-90, 90]")
    void boundingBoxClampedAtPole() {
        double[] box = GeoDistance.boundingBox(89.9, 0.0, 100.0);
        assertThat(box[1]).isLessThanOrEqualTo(90.0);
        assertThat(box[0]).isGreaterThanOrEqualTo(-90.0);
    }
}
