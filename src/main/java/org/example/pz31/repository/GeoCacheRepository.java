package org.example.pz31.repository;

import org.example.pz31.model.GeoCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeoCacheRepository extends JpaRepository<GeoCache, Long> {

    Optional<GeoCache> findByAddressKey(String addressKey);
}
