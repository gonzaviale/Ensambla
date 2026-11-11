package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;

import java.util.Optional;

public interface ConversacionClienteRepository {

    Optional<ConversacionCliente> findByClienteId(ClienteId clienteId);

    ConversacionCliente save(ConversacionCliente conversacion);
}
