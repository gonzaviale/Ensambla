package com.ensambla.backend.domain.useCase.proyecto.crearProyectoDesdePlantilla;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record CrearProyectoDesdePlantillaRequest(UsuarioId usuarioId, PlantillaId plantillaId) {
}

