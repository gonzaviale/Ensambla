package com.ensambla.backend.domain.dto.campo;

import java.util.UUID;

public record ListarCamposRequest(UUID usuarioId, UUID bloqueId) {
}
