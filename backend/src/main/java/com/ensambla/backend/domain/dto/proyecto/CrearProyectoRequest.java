package com.ensambla.backend.domain.dto.proyecto;

import java.util.UUID;

public record CrearProyectoRequest(UUID usuarioId, String nombre, String descripcion) {
}

