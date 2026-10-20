package com.ensambla.backend.infrastructure.ia;

import java.util.List;

/** Forma del JSON que devuelve el modelo ({@code ia/esquema-sami.json}), antes de validarlo. */
record PropuestaSamiJson(String plantilla, Double confianza, List<ColumnaJson> columnas) {

    record ColumnaJson(String nombre, String tipo, List<String> opciones) {
    }
}
