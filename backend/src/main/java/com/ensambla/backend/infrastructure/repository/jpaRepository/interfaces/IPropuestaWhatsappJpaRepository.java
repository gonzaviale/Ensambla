package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.PropuestaWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPropuestaWhatsappJpaRepository extends JpaRepository<PropuestaWhatsappEntity, UUID> {

    Optional<PropuestaWhatsappEntity> findByTelefonoAndEstadoIn(String telefono,
                                                                Collection<EstadoPropuestaWhatsapp> estados);
}
