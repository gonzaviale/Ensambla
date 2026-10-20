package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.UUID;

public record ConversarConSamiRequest(UUID usuarioId, String texto, List<RespuestaSamiItem> respuestas) {
}
