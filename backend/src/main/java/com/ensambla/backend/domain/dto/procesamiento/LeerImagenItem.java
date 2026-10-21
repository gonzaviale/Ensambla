package com.ensambla.backend.domain.dto.procesamiento;

import java.util.List;
import java.util.Map;

/**
 * Lo que se leyó de la foto.
 *
 * @param filas        una por renglón; sus datos van por la clave del campo del bloque o la de una columna nueva
 * @param camposNuevos columnas de la foto que el bloque no tiene; vacía si el bloque no existe todavía
 * @param advertencias avisos de la lectura para mostrar a la persona
 */
public record LeerImagenItem(List<Map<String, DatoItem>> filas, List<CampoItem> camposNuevos,
                             List<String> advertencias) {

    /** @param dudoso {@code true} si la IA no leyó el dato con seguridad */
    public record DatoItem(String valor, boolean dudoso) {
    }

    /** @param tipo nombre de un {@code TipoCampo} */
    public record CampoItem(String clave, String nombre, String tipo, List<String> opciones) {
    }
}
