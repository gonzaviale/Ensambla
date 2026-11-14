package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IRespuestaMiniObjetivoJpaRepository extends JpaRepository<RespuestaMiniObjetivoEntity, UUID> {

    boolean existsByMiniObjetivoId(UUID miniObjetivoId);
}
