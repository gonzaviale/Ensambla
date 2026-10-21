package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.UUID;

/**
 * @param imagen la foto que adjuntó la persona en este turno; {@code null} si no adjuntó ninguna
 */
public record ConversarEnTallerRequest(
        UUID usuarioId,
        String mensaje,
        EstadoTallerItem estado,
        List<MensajeTallerItem> historial,
        ImagenTallerItem imagen
) {
}
