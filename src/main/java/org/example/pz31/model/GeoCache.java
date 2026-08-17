package org.example.pz31.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Кеш геокодування у БД: та сама адреса не має ходити у зовнішній API двічі.
 * Кеш переживає перезапуск застосунку (на відміну від in-memory Spring Cache).
 * Зберігаємо і «негативний» результат (found=false), щоб не довбати API неіснуючими адресами.
 */
@Entity
@Table(name = "geo_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Нормалізований (lower-case, trimmed) ключ адреси. */
    @Column(nullable = false, unique = true, length = 512)
    private String addressKey;

    private Double latitude;

    private Double longitude;

    /** true — адресу знайдено; false — Nominatim відповів, але нічого не знайшов. */
    @Column(nullable = false)
    private boolean found;

    @Column(nullable = false)
    private Instant cachedAt;
}
