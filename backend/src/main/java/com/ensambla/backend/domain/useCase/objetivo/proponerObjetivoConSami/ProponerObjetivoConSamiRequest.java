package com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami;

import com.ensambla.backend.domain.model.objetivo.MensajeObjetivo;
import com.ensambla.backend.domain.model.usuario.UsuarioId;

import java.util.List;

/** La conversación completa hasta ahora: el servidor no la guarda, así que se manda en cada pedido. */
public record ProponerObjetivoConSamiRequest(UsuarioId usuarioId, List<MensajeObjetivo> mensajes) {
}
