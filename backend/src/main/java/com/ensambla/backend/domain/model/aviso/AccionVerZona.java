package com.ensambla.backend.domain.model.aviso;

/** Lleva a la pantalla de la zona filtrando por un producto; no cambia ningún dato. */
public record AccionVerZona(String texto, String producto) implements AccionDeAviso {
}
