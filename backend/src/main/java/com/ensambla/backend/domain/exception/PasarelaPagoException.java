package com.ensambla.backend.domain.exception;

public class PasarelaPagoException extends RuntimeException {

    public PasarelaPagoException() {
        super("No pudimos confirmar la operación con Mercado Pago. Actualizá el estado antes de volver a intentar.");
    }
}
