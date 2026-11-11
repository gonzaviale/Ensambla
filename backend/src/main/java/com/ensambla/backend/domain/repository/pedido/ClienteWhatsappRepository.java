package com.ensambla.backend.domain.repository.pedido;

import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClienteWhatsappRepository {

    Optional<ClienteWhatsapp> findByTelefono(String telefono);

    ClienteWhatsapp save(ClienteWhatsapp cliente);

    /** Los clientes con esos ids; los que no existen no vienen. */
    List<ClienteWhatsapp> findAllByIds(Collection<ClienteId> ids);
}
