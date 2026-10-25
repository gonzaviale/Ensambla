package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;

import java.util.List;

public record BloqueLectura(BloqueId id, String nombre, List<CampoConocido> campos) {

    public BloqueLectura {
        campos = List.copyOf(campos);
    }
}
