package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CierreCajaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICierreCajaJpaRepository extends JpaRepository<CierreCajaEntity, UUID> {

    Optional<CierreCajaEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<CierreCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnDescIdDesc(
            UUID negocioId, UUID proyectoId, UUID inventarioId, Pageable pagina);
}
