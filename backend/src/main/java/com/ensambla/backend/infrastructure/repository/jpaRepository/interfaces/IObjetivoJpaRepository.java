package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IObjetivoJpaRepository extends JpaRepository<ObjetivoEntity, UUID> {

    /** Nunca lista objetivos de otro negocio. */
    List<ObjetivoEntity> findAllByNegocioIdOrderByCreadoEnAscIdAsc(UUID negocioId);

    /** Los negocios que tienen objetivos, sin repetir y en un orden fijo para poder paginar. */
    @Query("select distinct o.negocioId from ObjetivoEntity o order by o.negocioId")
    List<UUID> findNegocioIdsConObjetivos(Pageable pageable);

    /** Un objetivo de otro negocio no se encuentra, igual que uno que no existe. */
    Optional<ObjetivoEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ObjetivoEntity o where o.id = :id and o.negocioId = :negocioId")
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
