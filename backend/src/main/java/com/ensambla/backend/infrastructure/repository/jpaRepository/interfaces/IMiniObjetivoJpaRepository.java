package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMiniObjetivoJpaRepository extends JpaRepository<MiniObjetivoEntity, UUID> {

    /** Nunca lista mini-objetivos de otro negocio. */
    List<MiniObjetivoEntity> findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(
            UUID objetivoId, UUID negocioId);

    /** Un mini-objetivo de otro objetivo o de otro negocio no se encuentra, igual que uno que no existe. */
    Optional<MiniObjetivoEntity> findByIdAndObjetivoIdAndNegocioId(UUID id, UUID objetivoId, UUID negocioId);
}
