package com.ensambla.backend.domain.model.procesamiento;

import java.util.List;
import java.util.UUID;

public record BloqueLectura(UUID id, String nombre, List<CampoConocido> campos) {

    public BloqueLectura {
        campos = List.copyOf(campos);
    }
}
