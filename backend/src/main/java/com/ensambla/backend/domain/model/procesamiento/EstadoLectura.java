package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.enums.EstadoLote;

import java.util.UUID;

public record EstadoLectura(UUID id, EstadoLote estado, String motivoError, int intentos, PropuestaLectura propuesta) {
}
