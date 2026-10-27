package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IVinculoWhatsappJpaRepository extends JpaRepository<VinculoWhatsappEntity, UUID> {

    Optional<VinculoWhatsappEntity> findByUsuarioId(UUID usuarioId);

    Optional<VinculoWhatsappEntity> findByTelefono(String telefono);

    /** Borra en el momento: con el borrado derivado, Hibernate inserta antes de borrar y chocaría con la unicidad. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VinculoWhatsappEntity v where v.usuarioId = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
