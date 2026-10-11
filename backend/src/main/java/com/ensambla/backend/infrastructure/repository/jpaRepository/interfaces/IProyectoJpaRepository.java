package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IProyectoJpaRepository extends JpaRepository<ProyectoEntity, UUID> {

    /** Nunca lista proyectos de otro negocio. */
    List<ProyectoEntity> findAllByNegocioIdOrderByCreadoEnDescIdAsc(UUID negocioId);

    Optional<ProyectoEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** Borrado directo: las FK de PostgreSQL eliminan bloques, campos y registros en cascada. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProyectoEntity p where p.id = :id and p.negocioId = :negocioId")
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
