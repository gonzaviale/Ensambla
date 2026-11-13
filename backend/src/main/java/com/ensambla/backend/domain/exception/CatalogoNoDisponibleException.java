package com.ensambla.backend.domain.exception;

public class CatalogoNoDisponibleException extends DomainException {

    public CatalogoNoDisponibleException() {
        super("El catálogo no está disponible en este momento. Podés completar el producto a mano.");
    }
}
