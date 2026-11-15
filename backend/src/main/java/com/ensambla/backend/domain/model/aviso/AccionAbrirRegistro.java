package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

/** Lleva a la fila del producto; no cambia ningún dato. */
public record AccionAbrirRegistro(String texto, ProyectoId proyectoId, BloqueId bloqueId, RegistroId registroId)
        implements AccionDeAviso {
}
