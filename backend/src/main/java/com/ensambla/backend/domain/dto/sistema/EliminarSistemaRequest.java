package com.ensambla.backend.domain.dto.sistema;

import java.util.UUID;

public record EliminarSistemaRequest(UUID usuarioId, String id) {
}
