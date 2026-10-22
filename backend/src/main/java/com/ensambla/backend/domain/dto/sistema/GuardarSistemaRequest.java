package com.ensambla.backend.domain.dto.sistema;

import java.util.UUID;

/** {@code id} es el de la URL y manda sobre el que traiga el cuerpo. */
public record GuardarSistemaRequest(UUID usuarioId, String id, SistemaItem sistema) {
}
