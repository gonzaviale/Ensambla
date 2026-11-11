package com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

/**
 * Pedido del panel de la home para un sistema (proyecto). {@code dias} son los últimos días a mirar, contando hoy; sin
 * valor son 30.
 */
public record ObtenerPanelNegocioRequest(UsuarioId usuarioId, ProyectoId proyectoId, Integer dias) {
}
