package org.example.pz31.repository;

import org.example.pz31.model.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Page<Place> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<Place> findByNameContainingIgnoreCase(String namePart, Pageable pageable);

    Page<Place> findByCategoryIgnoreCaseAndNameContainingIgnoreCase(String category, String namePart, Pageable pageable);


    @Query("""
            SELECT p FROM Place p
            WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL
              AND p.latitude BETWEEN :minLat AND :maxLat
              AND p.longitude BETWEEN :minLon AND :maxLon
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
            """)
    List<Place> findWithinBoundingBox(@Param("minLat") double minLat,
                                      @Param("maxLat") double maxLat,
                                      @Param("minLon") double minLon,
                                      @Param("maxLon") double maxLon,
                                      @Param("category") String category);
}
