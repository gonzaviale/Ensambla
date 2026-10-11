package com.ensambla.backend.infrastructure.ia;

import java.util.List;
import java.util.Map;

/** Forma del JSON que devuelve el modelo ({@code ia/esquema-lectura-cuaderno.json}), antes de validarlo. */
record LecturaIaJson(String tipoRegistro, BloqueJson bloque, List<FilaJson> filas, List<String> advertencias) {

    record BloqueJson(String nombre, String bloqueExistenteId, List<CampoJson> campos) {
    }

    record CampoJson(String clave, String nombre, String tipo, List<String> opciones) {
    }

    record FilaJson(Integer pagina, Integer renglon, Map<String, DatoJson> datos) {
    }

    record DatoJson(String valor, Double confianza) {
    }
}
