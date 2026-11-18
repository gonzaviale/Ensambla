package com.ensambla.backend.domain.model.sistema;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
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

    /** Las columnas del bloque; ninguna si el sistema ya no tiene ese bloque. */
    public List<Campo> columnasDe(BloqueId bloqueId) {
        return proyecto.getBloques().stream().filter(bloque -> bloque.getId().equals(bloqueId))
                .map(Bloque::getCampos).findFirst().orElse(List.of());
    }
}
