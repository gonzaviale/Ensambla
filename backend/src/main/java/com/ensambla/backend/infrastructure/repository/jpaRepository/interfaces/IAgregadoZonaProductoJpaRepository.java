package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.AgregadoZonaProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IAgregadoZonaProductoJpaRepository extends JpaRepository<AgregadoZonaProductoEntity, UUID> {

    List<AgregadoZonaProductoEntity> findAllByZonaIdOrderByNombreAscIdAsc(UUID zonaId);
}
