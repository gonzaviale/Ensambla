package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Stock actual de los productos (registros) de un bloque, con la misma búsqueda y paginación que el listado. */
public record ConsultarStockRequest(UsuarioId usuarioId, BloqueId bloqueId, String texto, Integer pagina,
        Integer tamanio) {
}
