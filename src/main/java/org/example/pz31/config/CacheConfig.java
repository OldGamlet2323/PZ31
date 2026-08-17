package org.example.pz31.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Вмикає Spring Cache. Основний кеш геокодування — у БД (GeoCache),
 * але @EnableCaching дає ще й in-memory шар (@Cacheable) поверх БД-кешу
 * для «гарячих» адрес у межах одного запуску.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
