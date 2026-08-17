package org.example.pz31.repository;

import org.example.pz31.model.Place;
import org.example.pz31.service.GeoDistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// У Spring Boot 4.1 slice @DataJpaTest винесено з test-autoconfigure,
// тому використовуємо повний контекст на H2 з відкатом транзакцій (@Transactional).
@SpringBootTest
@Transactional
@DisplayName("PlaceRepository — запити CRUD, фільтри та bounding box")
class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository repository;

    private Place newPlace(String name, String category, Double lat, Double lon) {
        return Place.builder()
                .name(name).address(name + " address").category(category)
                .latitude(lat).longitude(lon)
                .geocoded(lat != null)
                .createdAt(Instant.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save + findById зберігає й повертає місце")
    void saveAndFind() {
        Place saved = repository.save(newPlace("Cafe", "cafe", 50.45, 30.52));
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("findByCategoryIgnoreCase фільтрує без урахування регістру + пагінація")
    void findByCategory() {
        repository.save(newPlace("A", "Cafe", 50.0, 30.0));
        repository.save(newPlace("B", "cafe", 50.1, 30.1));
        repository.save(newPlace("C", "park", 50.2, 30.2));

        Page<Place> page = repository.findByCategoryIgnoreCase("CAFE", PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase шукає за частиною назви")
    void findByNamePart() {
        repository.save(newPlace("Coffee House", "cafe", 50.0, 30.0));
        repository.save(newPlace("Tea Room", "cafe", 50.1, 30.1));

        Page<Place> page = repository.findByNameContainingIgnoreCase("coffee", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Coffee House");
    }

    @Test
    @DisplayName("Пагінація повертає правильний розмір сторінки")
    void pagination() {
        for (int i = 0; i < 25; i++) {
            repository.save(newPlace("P" + i, "cafe", 50.0 + i * 0.001, 30.0));
        }
        Page<Place> page = repository.findByCategoryIgnoreCase("cafe", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("findWithinBoundingBox відбирає лише кандидатів у межах і з координатами")
    void boundingBoxQuery() {
        // У межах ~10 км від Києва
        repository.save(newPlace("Near", "cafe", 50.4510, 30.5240));
        // Далеко (Львів)
        repository.save(newPlace("Far", "cafe", 49.8397, 24.0297));
        // Без координат — не має потрапити
        repository.save(newPlace("NoCoords", "cafe", null, null));

        double[] box = GeoDistance.boundingBox(50.4501, 30.5234, 10.0);
        List<Place> result = repository.findWithinBoundingBox(box[0], box[1], box[2], box[3], null);

        assertThat(result).extracting(Place::getName).containsExactly("Near");
    }

    @Test
    @DisplayName("findWithinBoundingBox з фільтром категорії")
    void boundingBoxWithCategory() {
        repository.save(newPlace("Cafe1", "cafe", 50.4510, 30.5240));
        repository.save(newPlace("Park1", "park", 50.4512, 30.5242));

        double[] box = GeoDistance.boundingBox(50.4501, 30.5234, 10.0);
        List<Place> result = repository.findWithinBoundingBox(box[0], box[1], box[2], box[3], "cafe");

        assertThat(result).extracting(Place::getName).containsExactly("Cafe1");
    }
}
