package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.util.Map;

/** Suma una fila a la tabla indicada; los valores van por clave semántica del campo. */
public record AccionCrearRegistro(String texto, ProyectoId proyectoId, BloqueId bloqueId,
                                  Map<String, Object> valores) implements AccionDeAviso {

    @Override
    public boolean modificaDatos() {
        return true;
    }

    @Override
    public void aplicarEn(AplicadorDeAcciones aplicador) {
        aplicador.crear(this);
    }
}
