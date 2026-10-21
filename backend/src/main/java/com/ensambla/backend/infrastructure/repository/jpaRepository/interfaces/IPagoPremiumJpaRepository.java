package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.PagoPremiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IPagoPremiumJpaRepository extends JpaRepository<PagoPremiumEntity, String> {

    Optional<PagoPremiumEntity> findFirstByNegocioIdAndEstadoAndInicioEnLessThanEqualAndFinEnGreaterThanOrderByFinEnDesc(
            UUID negocioId, String estado, OffsetDateTime inicio, OffsetDateTime fin);

    boolean existsByNegocioId(UUID negocioId);
}
