package org.example.pz31.exception;

/**
 * Зовнішній гео-сервіс (Nominatim) недоступний / повернув помилку / таймаут.
 * → HTTP 503 (а не 500). Місце при цьому все одно зберігається без координат.
 */
public class GeocodingUnavailableException extends RuntimeException {
    public GeocodingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeocodingUnavailableException(String message) {
        super(message);
    }
}
