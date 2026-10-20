package com.ensambla.backend.domain.dto.lectura;

import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.DatoLeido;

import java.util.List;
import java.util.Map;

/**
 * Lo que se leyó de la foto.
 *
 * @param filas         una por renglón; sus datos van por la clave del campo del bloque o la de una columna nueva
 * @param camposNuevos  columnas de la foto que el bloque no tiene; vacía si el bloque no existe todavía
 * @param advertencias  avisos de la lectura para mostrar a la persona
 */
public record LeerImagenItem(List<Map<String, DatoLeido>> filas, List<CampoPropuesto> camposNuevos,
                             List<String> advertencias) {

    public LeerImagenItem {
        filas = List.copyOf(filas);
        camposNuevos = List.copyOf(camposNuevos);
        advertencias = List.copyOf(advertencias);
    }
}
