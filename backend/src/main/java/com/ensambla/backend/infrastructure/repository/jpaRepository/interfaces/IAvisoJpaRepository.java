package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.enums.EstadoAviso;
import com.ensambla.backend.domain.model.enums.TipoAviso;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAvisoJpaRepository extends JpaRepository<AvisoEntity, UUID> {

    Optional<AvisoEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<AvisoEntity> findByNegocioIdAndHuella(UUID negocioId, String huella);

    List<AvisoEntity> findByNegocioIdAndTipoAndEstado(UUID negocioId, TipoAviso tipo, EstadoAviso estado);

    List<AvisoEntity> findByNegocioId(UUID negocioId);

    List<AvisoEntity> findByNegocioIdAndEstado(UUID negocioId, EstadoAviso estado);
}
