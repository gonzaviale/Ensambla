package com.ensambla.backend.domain.dto.registro;

import java.util.List;
import java.util.UUID;

/** Filas de un bloque para elegir en un campo RELACION: por texto libre o, si vienen {@code ids}, sólo esas. */
public record ListarReferenciasRequest(
        UUID usuarioId,
        UUID bloqueId,
        String texto,
        List<UUID> ids) {
}
