package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RenovacionPremiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRenovacionPremiumJpaRepository extends JpaRepository<RenovacionPremiumEntity, UUID> {

    Optional<RenovacionPremiumEntity> findByProveedorId(String proveedorId);

    @Query("SELECT r.negocioId FROM RenovacionPremiumEntity r WHERE r.id = :id")
    Optional<UUID> findNegocioIdById(UUID id);

    /** El contrato abierto tiene prioridad aun cuando dos creaciones compartan el mismo instante. */
    @Query(value = "SELECT * FROM renovacion_premium WHERE negocio_id = :negocioId "
            + "ORDER BY (estado <> 'CANCELADA') DESC, creado_en DESC, id DESC LIMIT 1", nativeQuery = true)
    Optional<RenovacionPremiumEntity> findActualByNegocioId(UUID negocioId);

    boolean existsByNegocioIdAndEstadoNot(UUID negocioId, String estado);

    /** Orden rotativo y lote acotado para recuperar también notificaciones de contratos cancelados. */
    @Query(value = "SELECT id FROM renovacion_premium ORDER BY sincronizado_en NULLS FIRST, id LIMIT 100",
            nativeQuery = true)
    List<UUID> listarParaSincronizar();
}
