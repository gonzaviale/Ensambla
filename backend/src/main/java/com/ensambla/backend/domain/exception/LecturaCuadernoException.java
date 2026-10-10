package com.ensambla.backend.domain.exception;

/** La IA no pudo leer las páginas o devolvió algo que no respeta el contrato de lectura. */
public class LecturaCuadernoException extends DomainException {

    private final boolean recuperable;

    public LecturaCuadernoException(String message) {
        this(message, null, true);
    }

    public LecturaCuadernoException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public LecturaCuadernoException(String message, Throwable cause, boolean recuperable) {
        super(message, cause);
        this.recuperable = recuperable;
    }

    public boolean esRecuperable() {
        return recuperable;
    }
}
