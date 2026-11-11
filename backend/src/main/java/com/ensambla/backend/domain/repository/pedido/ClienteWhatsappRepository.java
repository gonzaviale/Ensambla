package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;

import java.util.Optional;

public interface ClienteWhatsappRepository {

    Optional<ClienteWhatsapp> findByTelefono(String telefono);

    ClienteWhatsapp save(ClienteWhatsapp cliente);
}
