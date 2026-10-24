package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.List;
import java.util.UUID;

/** @param bloqueRelacionadoId tabla a la que apunta un campo RELACION; {@code null} en los demás tipos */
public record CampoConocido(UUID id, String nombre, TipoCampo tipo, List<String> opciones, UUID bloqueRelacionadoId) {

    public CampoConocido {
        opciones = List.copyOf(opciones);
    }

    public CampoConocido(UUID id, String nombre, TipoCampo tipo, List<String> opciones) {
        this(id, nombre, tipo, opciones, null);
    }
}
