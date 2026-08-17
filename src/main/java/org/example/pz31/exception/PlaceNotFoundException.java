package org.example.pz31.exception;

/** Місце з таким id не знайдено → HTTP 404. */
public class PlaceNotFoundException extends RuntimeException {
    public PlaceNotFoundException(Long id) {
        super("Місце з id=" + id + " не знайдено");
    }
}
