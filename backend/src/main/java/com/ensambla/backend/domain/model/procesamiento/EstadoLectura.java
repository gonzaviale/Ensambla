package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lote.EstadoLote;

import com.ensambla.backend.domain.model.lote.LoteId;

public record EstadoLectura(LoteId id, EstadoLote estado, String motivoError, int intentos,
        PropuestaLectura propuesta) {
}
