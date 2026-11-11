package com.ensambla.backend.domain.useCase.orden.cambiarEstadoOrden;

import com.ensambla.backend.domain.model.pedido.EstadoOrdenCompra;
import com.ensambla.backend.domain.model.pedido.OrdenCompraId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** @param motivo por qué se rechaza la orden; sólo tiene efecto al rechazar y puede faltar */
public record CambiarEstadoOrdenRequest(UsuarioId usuarioId, OrdenCompraId ordenId, EstadoOrdenCompra estado,
                                        String motivo) {
}
