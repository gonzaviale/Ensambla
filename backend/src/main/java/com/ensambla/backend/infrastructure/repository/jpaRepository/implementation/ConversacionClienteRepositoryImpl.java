package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.pedido.ClienteId;
import com.ensambla.backend.domain.model.pedido.ConversacionCliente;
import com.ensambla.backend.domain.repository.pedido.ConversacionClienteRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IConversacionClienteJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.ConversacionClienteRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConversacionClienteRepositoryImpl implements ConversacionClienteRepository {

    private final IConversacionClienteJpaRepository jpa;
    private final ConversacionClienteRepositoryMapper mapper;

    @Override
    public Optional<ConversacionCliente> findByClienteId(ClienteId clienteId) {
        return jpa.findByClienteId(clienteId.valor()).map(mapper::toDomain);
    }

    @Override
    public ConversacionCliente save(ConversacionCliente conversacion) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(conversacion)));
    }
}
