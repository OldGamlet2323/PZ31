package org.example.pz31.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запит на створення/оновлення місця.
 * Координати необов'язкові: якщо їх не передали — сервіс геокодує адресу через Nominatim.
 * Якщо передали — беремо як є (ручний ввід має пріоритет і не ходить в API).
 */
public record PlaceRequest(

        @NotBlank(message = "name є обов'язковим")
        @Size(max = 255, message = "name задовгий (макс. 255)")
        String name,

        @NotBlank(message = "address є обов'язковим")
        @Size(max = 512, message = "address задовгий (макс. 512)")
        String address,

        @Size(max = 100, message = "category задовга (макс. 100)")
        String category,

        @DecimalMin(value = "-90.0", message = "latitude має бути в діапазоні [-90, 90]")
        @DecimalMax(value = "90.0", message = "latitude має бути в діапазоні [-90, 90]")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "longitude має бути в діапазоні [-180, 180]")
        @DecimalMax(value = "180.0", message = "longitude має бути в діапазоні [-180, 180]")
        Double longitude
) {
    public boolean hasManualCoordinates() {
        return latitude != null && longitude != null;
    }
}
