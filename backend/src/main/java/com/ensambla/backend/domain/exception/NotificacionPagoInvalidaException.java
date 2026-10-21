package com.ensambla.backend.domain.exception;

public class NotificacionPagoInvalidaException extends RuntimeException {

    public NotificacionPagoInvalidaException() {
        super("Notificación de pago inválida.");
    }
}
