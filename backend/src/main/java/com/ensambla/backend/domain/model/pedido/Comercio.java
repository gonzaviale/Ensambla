package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;

/**
 * Un comercio al que un cliente puede pedirle por WhatsApp: un negocio con su número de WhatsApp vinculado.
 *
 * @param id     el negocio
 * @param nombre cómo lo conoce el cliente
 */
public record Comercio(NegocioId id, String nombre) {
}
