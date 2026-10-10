package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ICampoJpaRepository extends JpaRepository<CampoEntity, UUID> {

    List<CampoEntity> findAllByBloqueIdInAndNegocioIdOrderByPosicionAscIdAsc(List<UUID> bloqueIds, UUID negocioId);
}
