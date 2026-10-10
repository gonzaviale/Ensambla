package com.ensambla.backend.domain.exception;

/** La IA no pudo leer las páginas o devolvió algo que no respeta el contrato de lectura. */
public class LecturaCuadernoException extends DomainException {

    public LecturaCuadernoException(String message) {
        super(message);
    }

    public LecturaCuadernoException(String message, Throwable cause) {
        super(message, cause);
    }
}
