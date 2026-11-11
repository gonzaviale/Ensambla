package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pedido.ClienteWhatsapp;
import com.ensambla.backend.domain.repository.pedido.ClienteWhatsappRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IClienteWhatsappJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PedidoRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClienteWhatsappRepositoryImpl implements ClienteWhatsappRepository {

    private final IClienteWhatsappJpaRepository jpa;
    private final PedidoRepositoryMapper mapper;

    @Override
    public Optional<ClienteWhatsapp> findByTelefono(String telefono) {
        return jpa.findByTelefono(telefono).map(mapper::toDomain);
    }

    @Override
    public ClienteWhatsapp save(ClienteWhatsapp cliente) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(cliente)));
    }
}
