package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.MetricaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMetricaJpaRepository extends JpaRepository<MetricaEntity, UUID> {

    /** Nunca lista métricas de otro negocio ni de otro cuaderno. */
    List<MetricaEntity> findAllByProyectoIdAndNegocioIdOrderByOrdenAscIdAsc(UUID proyectoId, UUID negocioId);

    /** Una métrica de otro negocio o de otro cuaderno no se encuentra, igual que una que no existe. */
    Optional<MetricaEntity> findByIdAndProyectoIdAndNegocioId(UUID id, UUID proyectoId, UUID negocioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MetricaEntity m where m.id = :id and m.negocioId = :negocioId")
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
