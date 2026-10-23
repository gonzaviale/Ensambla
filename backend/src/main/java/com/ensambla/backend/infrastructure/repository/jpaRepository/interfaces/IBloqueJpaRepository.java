package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IBloqueJpaRepository extends JpaRepository<BloqueEntity, UUID> {

    Optional<BloqueEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<BloqueEntity> findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(UUID proyectoId, UUID negocioId);

    List<BloqueEntity> findAllByNegocioIdOrderByPosicionAscIdAsc(UUID negocioId);

    /** Borrado directo: las FK de PostgreSQL eliminan campos y registros en cascada. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from BloqueEntity b where b.id = :id and b.negocioId = :negocioId")
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
