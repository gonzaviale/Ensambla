package com.ensambla.backend.domain.useCase.registro.eliminarRegistro;

import java.util.UUID;

public record EliminarRegistroRequest(UUID usuarioId, UUID registroId) {
}
