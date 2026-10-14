package com.ensambla.backend.domain.dto.inventario;

import java.util.UUID;

/** Últimos movimientos de los productos de un bloque; con {@code registroId} sólo los de ese producto. */
public record ListarMovimientosRequest(UUID usuarioId, UUID bloqueId, UUID registroId) {
}
