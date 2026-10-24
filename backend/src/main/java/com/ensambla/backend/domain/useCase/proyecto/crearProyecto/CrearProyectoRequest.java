package com.ensambla.backend.domain.useCase.proyecto.crearProyecto;

import java.util.UUID;

public record CrearProyectoRequest(UUID usuarioId, String nombre, String descripcion) {
}

