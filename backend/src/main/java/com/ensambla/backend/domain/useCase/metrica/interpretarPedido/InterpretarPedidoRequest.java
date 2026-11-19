package com.ensambla.backend.domain.useCase.metrica.interpretarPedido;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/** @param pedido lo que la persona escribió, por ejemplo «cuánto vendí esta semana» */
public record InterpretarPedidoRequest(UsuarioId usuarioId, ProyectoId proyectoId, String pedido) {
}
