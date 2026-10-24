package com.ensambla.backend.domain.useCase.registro.listarRegistros;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarRegistrosRequest(
        UsuarioId usuarioId,
        BloqueId bloqueId,
        String texto,
        String orden,
        String direccion,
        Integer pagina,
        Integer tamanio) {
}
