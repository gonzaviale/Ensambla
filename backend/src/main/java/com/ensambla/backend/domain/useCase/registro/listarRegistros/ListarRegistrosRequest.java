package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import java.util.UUID;

public record ListarRegistrosRequest(
        UUID usuarioId,
        UUID bloqueId,
        String texto,
        String orden,
        String direccion,
        Integer pagina,
        Integer tamanio) {
}
