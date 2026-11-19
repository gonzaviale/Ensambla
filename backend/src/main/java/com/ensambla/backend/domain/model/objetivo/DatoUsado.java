package com.ensambla.backend.domain.model.objetivo;

/** Una fuente de datos y si el negocio tiene algo de ella: la persona ve en qué se apoya Sami y qué no tiene. */
public record DatoUsado(FuenteDato fuente, boolean hay) {
}
