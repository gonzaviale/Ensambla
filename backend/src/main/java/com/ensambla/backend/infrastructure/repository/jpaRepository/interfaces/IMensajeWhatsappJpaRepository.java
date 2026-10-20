package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.MensajeWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IMensajeWhatsappJpaRepository extends JpaRepository<MensajeWhatsappEntity, UUID> {

    boolean existsByIdExterno(String idExterno);
}
