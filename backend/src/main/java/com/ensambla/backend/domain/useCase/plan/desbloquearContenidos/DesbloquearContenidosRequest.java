package com.ensambla.backend.domain.useCase.plan.desbloquearContenidos;

import java.util.List;
import java.util.UUID;

public record DesbloquearContenidosRequest(UUID usuarioId, String tipo, List<String> claves) {
}
