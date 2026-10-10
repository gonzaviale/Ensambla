package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;

import java.util.Map;
import java.util.UUID;

public record PropuestaLectura(LecturaCuaderno lectura, Map<String, UUID> mapeoCampos) {

    public PropuestaLectura {
        mapeoCampos = Map.copyOf(mapeoCampos);
    }
}
