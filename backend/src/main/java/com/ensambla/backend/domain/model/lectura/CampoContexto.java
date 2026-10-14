package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;

/** Campo de un bloque que el negocio ya tiene, tal como se le muestra a la IA: nombre, tipo y opciones. */
public record CampoContexto(String nombre, TipoCampo tipo, List<String> opciones) {

    public CampoContexto {
        opciones = List.copyOf(opciones);
    }
}
