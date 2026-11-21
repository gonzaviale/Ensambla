package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IReajusteMiniObjetivoJpaRepository extends JpaRepository<ReajusteMiniObjetivoEntity, UUID> {

    /** Los reajustes de esos resúmenes, en el orden en que se hicieron. */
    List<ReajusteMiniObjetivoEntity> findAllByResumenIdInOrderByCreadoEnAscIdAsc(Collection<UUID> resumenIds);
}
