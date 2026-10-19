package com.ensambla.backend.domain.dto.aviso;

import com.ensambla.backend.domain.model.enums.EstadoAviso;

import java.util.UUID;

/** {@code estado} es opcional: sin estado se listan todos los avisos del negocio. */
public record ListarAvisosRequest(UUID usuarioId, EstadoAviso estado) {
}
