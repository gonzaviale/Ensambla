package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.model.pedido.Comercio;

import java.util.List;

public interface ComercioRepository {

    /**
     * Los comercios a los que un cliente puede pedirle por WhatsApp: los negocios con un número vinculado, ordenados por
     * nombre.
     */
    List<Comercio> listarConWhatsapp();
}
