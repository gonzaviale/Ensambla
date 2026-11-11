package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ClienteWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IClienteWhatsappJpaRepository extends JpaRepository<ClienteWhatsappEntity, UUID> {

    Optional<ClienteWhatsappEntity> findByTelefono(String telefono);
}
