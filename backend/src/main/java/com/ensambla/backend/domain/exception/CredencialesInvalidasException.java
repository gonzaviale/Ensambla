package com.ensambla.backend.domain.exception;

public class CredencialesInvalidasException extends DomainException {

    public CredencialesInvalidasException() {
        super("No se pudo autenticar el acceso");
    }
}
