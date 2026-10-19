package com.ensambla.backend.infrastructure.presentation.dto.importacion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Lo leído de la foto, con la forma que usa la pantalla: tipos de campo ({@code text}, {@code number}...) y valores
 * ya convertidos (números, sí/no).
 *
 * @param filas        un mapa por renglón, indexado por la clave del campo
 * @param camposNuevos columnas de la foto que el bloque no tiene; sus valores van en cada fila
 * @param dudas        datos que la IA no leyó con seguridad: fila (desde 0), clave del campo y motivo
 */
public record LecturaImagenResponse(List<Map<String, Object>> filas, String descripcion,
                                    List<CampoResponse> camposNuevos, List<DudaResponse> dudas) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CampoResponse(String key, String label, String type, boolean required, List<String> options) {
    }

    public record DudaResponse(int fila, String campo, String motivo) {
    }
}
