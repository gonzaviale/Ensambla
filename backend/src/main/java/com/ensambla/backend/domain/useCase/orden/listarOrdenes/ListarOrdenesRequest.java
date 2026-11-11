package com.ensambla.backend.domain.useCase.orden.listarOrdenes;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** @param estado sólo las órdenes en ese estado; null para todas */
public record ListarOrdenesRequest(UsuarioId usuarioId, EstadoOrdenCompra estado) {
}
