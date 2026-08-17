package org.example.pz31.exception;

/**
 * Nominatim відповів коректно, але за адресою нічого не знайдено.
 * Використовується там, де координати обов'язкові (напр. окремий ендпоінт геокодування)
 * → HTTP 422. При створенні місця цей випадок НЕ є помилкою: місце зберігається без координат.
 */
public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String address) {
        super("Адресу не знайдено: " + address);
    }
}
