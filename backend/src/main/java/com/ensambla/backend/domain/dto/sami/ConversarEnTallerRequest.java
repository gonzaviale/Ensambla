package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.UUID;

public record ConversarEnTallerRequest(
        UUID usuarioId,
        String mensaje,
        EstadoTallerItem estado,
        List<MensajeTallerItem> historial
) {
}
