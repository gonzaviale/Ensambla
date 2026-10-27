package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que se le pide a la IA: pasar un texto escrito a mano a filas de alguna de las tablas del negocio.
 *
 * @param destino la tabla que la persona ya eligió; null si la IA tiene que decidir cuál corresponde
 */
public record ConsultaInterpretacion(String texto, List<TablaDisponible> tablas, LocalDate hoy, BloqueId destino) {

    public ConsultaInterpretacion {
        tablas = List.copyOf(tablas);
    }
}
