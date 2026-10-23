package com.ensambla.backend.domain.dto.sami;

/** La foto adjunta en un turno del taller, ya decodificada. */
public record ImagenTallerItem(String tipo, byte[] datos) {
}
