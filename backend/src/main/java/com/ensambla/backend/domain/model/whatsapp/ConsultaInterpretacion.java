package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Lo que se le pide a la IA: pasar un texto escrito a mano a filas de alguna de las tablas del negocio.
 *
 * @param destino la tabla que la persona ya eligió; null si la IA tiene que decidir cuál corresponde
 * @param nombres los nombres de las filas que ya tienen las tablas de cosas y las que se vinculan (productos,
 *                clientes), para que la IA use el mismo y no invente otro
 */
public record ConsultaInterpretacion(String texto, List<TablaDisponible> tablas, LocalDate hoy, BloqueId destino,
                                     Map<BloqueId, List<String>> nombres) {

    public ConsultaInterpretacion {
        tablas = List.copyOf(tablas);
        nombres = Map.copyOf(nombres);
    }

    /** Sin nombres de filas existentes. */
    public ConsultaInterpretacion(String texto, List<TablaDisponible> tablas, LocalDate hoy, BloqueId destino) {
        this(texto, tablas, hoy, destino, Map.of());
    }
}
