package com.ensambla.backend.domain.useCase.sami;

import com.ensambla.backend.domain.model.sami.ColumnaSugerida;

import java.util.List;

public record ColumnaSamiItem(String nombre, String tipo, List<String> opciones) {

    public static ColumnaSamiItem de(ColumnaSugerida columna) {
        return new ColumnaSamiItem(columna.nombre(), columna.tipo().name(), columna.opciones());
    }
}
