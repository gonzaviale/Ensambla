package com.ensambla.backend.domain.useCase.sistema.guardarSistema;

import com.ensambla.backend.domain.useCase.sistema.SistemaItem;

import java.util.UUID;

/** {@code id} es el de la URL y manda sobre el que traiga el cuerpo. */
public record GuardarSistemaRequest(UUID usuarioId, String id, SistemaItem sistema) {
}
