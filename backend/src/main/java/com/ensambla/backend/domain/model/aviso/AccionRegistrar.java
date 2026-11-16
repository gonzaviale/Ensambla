package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

/** Lleva a la tabla donde cargar una entrada; no cambia ningún dato. */
public record AccionRegistrar(String texto, ProyectoId proyectoId, BloqueId bloqueId) implements AccionDeAviso {
}
