package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;

import java.util.List;

/** Columnas sugeridas (configuraciones de campo sin guardar) para probar cómo se suman a un proyecto. */
public final class ColumnasDePrueba {

    private ColumnasDePrueba() {
    }

    public static ConfiguracionCampo unaColumna(String nombre, String tipo) {
        return unaColumna(nombre, tipo, null);
    }

    public static ConfiguracionCampo unaColumna(String nombre, String tipo, List<String> opciones) {
        return new ConfiguracionCampo(nombre, tipo, false, null, opciones, null, null);
    }
}
