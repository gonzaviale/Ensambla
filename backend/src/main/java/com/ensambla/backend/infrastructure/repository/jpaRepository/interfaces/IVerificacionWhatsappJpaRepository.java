package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.VerificacionWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IVerificacionWhatsappJpaRepository extends JpaRepository<VerificacionWhatsappEntity, UUID> {

    Optional<VerificacionWhatsappEntity> findByUsuarioId(UUID usuarioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VerificacionWhatsappEntity v where v.usuarioId = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);

    long countByTelefonoAndEnviadoEnGreaterThanEqual(String telefono, OffsetDateTime desde);
}
