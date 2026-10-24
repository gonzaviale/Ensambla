package com.ensambla.backend.domain.useCase.campo.listarCampos;

import java.util.UUID;

public record ListarCamposRequest(UUID usuarioId, UUID bloqueId) {
}
