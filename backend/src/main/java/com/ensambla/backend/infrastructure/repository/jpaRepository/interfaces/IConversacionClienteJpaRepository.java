package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ConversacionClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IConversacionClienteJpaRepository extends JpaRepository<ConversacionClienteEntity, UUID> {

    Optional<ConversacionClienteEntity> findByClienteId(UUID clienteId);
}
