package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.util.Map;

/** Cambia datos de una fila existente; los valores van por clave semántica del campo (por ejemplo {@code price}). */
public record AccionEditarRegistro(String texto, ProyectoId proyectoId, BloqueId bloqueId, RegistroId registroId,
                                   Map<String, Object> valores) implements AccionDeAviso {

    @Override
    public boolean modificaDatos() {
        return true;
    }

    @Override
    public void aplicarEn(AplicadorDeAcciones aplicador) {
        aplicador.editar(this);
    }
}
