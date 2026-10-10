package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;
import java.util.UUID;

public record CampoConocido(UUID id, String nombre, TipoCampo tipo, List<String> opciones) {

    public CampoConocido {
        opciones = List.copyOf(opciones);
    }
}
