package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import java.util.UUID;

/** Stock actual de los productos (registros) de un bloque, con la misma búsqueda y paginación que el listado. */
public record ConsultarStockRequest(UUID usuarioId, UUID bloqueId, String texto, Integer pagina, Integer tamanio) {
}
