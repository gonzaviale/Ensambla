package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CodigoVinculacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ICodigoVinculacionJpaRepository extends JpaRepository<CodigoVinculacionEntity, UUID> {

    Optional<CodigoVinculacionEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CodigoVinculacionEntity c where c.usuarioId = :usuarioId and c.usadoEn is null")
    void deleteSinUsarByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
