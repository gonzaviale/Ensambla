package com.ensambla.backend.domain.useCase.sistema.eliminarSistema;

import java.util.UUID;

public record EliminarSistemaRequest(UUID usuarioId, String id) {
}
