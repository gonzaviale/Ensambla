package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.SeccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ISeccionJpaRepository extends JpaRepository<SeccionEntity, UUID> {

    List<SeccionEntity> findAllByNegocioIdOrderByPosicionAscIdAsc(UUID negocioId);

    List<SeccionEntity> findAllByProyectoIdAndNegocioId(UUID proyectoId, UUID negocioId);
}
