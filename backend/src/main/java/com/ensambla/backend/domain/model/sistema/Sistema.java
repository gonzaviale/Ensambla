package com.ensambla.backend.domain.model.sistema;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.seccion.Seccion;

import java.util.List;
import java.util.Map;

/**
 * Lo que la persona ve como un sistema: el proyecto con sus secciones, sus bloques (que traen las columnas) y las
 * filas de cada bloque.
 *
 * @param registrosPorBloque filas de cada bloque, en el orden en que la persona las dejó
 */
public record Sistema(Proyecto proyecto, List<Seccion> secciones, Map<BloqueId, List<Registro>> registrosPorBloque) {

    public ProyectoId id() {
        return proyecto.getId();
    }
}
