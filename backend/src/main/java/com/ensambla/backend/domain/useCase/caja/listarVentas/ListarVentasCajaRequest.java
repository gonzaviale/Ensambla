package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

public record ListarVentasCajaRequest(UsuarioId usuarioId, ProyectoId proyectoId, BloqueId inventarioId) {
}
