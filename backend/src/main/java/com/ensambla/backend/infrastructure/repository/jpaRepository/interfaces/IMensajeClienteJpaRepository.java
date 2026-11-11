package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.infrastructure.repository.entity.MensajeClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMensajeClienteJpaRepository extends JpaRepository<MensajeClienteEntity, UUID> {

    boolean existsByIdExterno(String idExterno);

    /** El recibido más viejo, bloqueado para que otra atención simultánea pase de largo y tome el siguiente. */
    @Query(value = "SELECT * FROM mensaje_cliente WHERE estado = 'RECIBIDO' ORDER BY recibido_en, id "
            + "LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<MensajeClienteEntity> findSiguienteRecibidoBloqueado();

    List<MensajeClienteEntity> findByEstadoAndReclamadoHastaBefore(EstadoMensajeWhatsapp estado, OffsetDateTime ahora);
}
