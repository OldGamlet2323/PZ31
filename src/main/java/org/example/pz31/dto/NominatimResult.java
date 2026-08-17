package org.example.pz31.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Мінімальне відображення елемента відповіді Nominatim.
 * У JSON lat/lon приходять рядками — тому тип String, парсимо в клієнті.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResult(
        String lat,
        String lon,
        @JsonProperty("display_name") String displayName
) {
}
