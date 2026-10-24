package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.List;

/** @param bloqueRelacionadoId tabla a la que apunta un campo RELACION; {@code null} en los demás tipos */
public record CampoConocido(CampoId id, String nombre, TipoCampo tipo, List<String> opciones,
        BloqueId bloqueRelacionadoId) {

    public CampoConocido {
        opciones = List.copyOf(opciones);
    }

    public CampoConocido(CampoId id, String nombre, TipoCampo tipo, List<String> opciones) {
        this(id, nombre, tipo, opciones, null);
    }
}
