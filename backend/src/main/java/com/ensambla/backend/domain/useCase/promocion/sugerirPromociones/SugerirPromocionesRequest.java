package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** Pedido de las promociones que conviene hacer a partir de lo que vende un sistema (proyecto) del negocio. */
public record SugerirPromocionesRequest(UsuarioId usuarioId, ProyectoId proyectoId) {
}
