package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que se le muestra a la persona para revisar.
 *
 * @param mapeoCampos  clave de cada columna leída → id del campo del bloque existente que le corresponde
 * @param camposNuevos columnas de la foto que el bloque destino todavía no tiene, con el tipo sugerido; también están
 *                     entre los campos de la lectura, pero sin entrada en {@code mapeoCampos}. Las lecturas guardadas
 *                     antes de que existiera esta lista vuelven con {@code null}: se leen como vacía
 */
public record PropuestaLectura(LecturaCuaderno lectura, Map<String, UUID> mapeoCampos,
                               List<CampoPropuesto> camposNuevos) {

    public PropuestaLectura {
        mapeoCampos = Map.copyOf(mapeoCampos);
        camposNuevos = camposNuevos == null ? List.of() : List.copyOf(camposNuevos);
    }

    public PropuestaLectura(LecturaCuaderno lectura, Map<String, UUID> mapeoCampos) {
        this(lectura, mapeoCampos, List.of());
    }
}
