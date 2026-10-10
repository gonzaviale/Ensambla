package com.ensambla.backend.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String fieldName, Object fieldValue) {
        super(String.format("No se encontró un %s con %s %s", resource, fieldName, fieldValue));
    }
}
